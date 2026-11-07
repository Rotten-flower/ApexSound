"""Pin and verify optional development dependencies; never embed these JARs in the mod."""
from pathlib import Path
import concurrent.futures
import hashlib
import json
import urllib.request
import zipfile
import io

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "tools/tacz-ports.json"
USER_AGENT = "APEX-Sound-effects-development"

def read_json(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": USER_AGENT}), timeout=45) as response:
        return json.load(response)

def artifact(version_id, relative, purpose):
    version = read_json("https://api.modrinth.com/v2/version/" + version_id)
    file = next(file for file in version["files"] if file["primary"])
    return {"versionId": version_id, "version": version["version_number"], "purpose": purpose,
            "path": relative, "url": file["url"], "sha512": file["hashes"]["sha512"],
            "projectId": version["project_id"], "license": "GPL-3.0-only" if purpose == "TaCZ port" else "MPL-2.0"}

def configure():
    platforms = {"neoforge-1.21.1": "62q6mB5Q", "fabric-1.20.1": "3ShZi2cZ", "fabric-1.21.1": "veM4XOhW"}
    entries = []
    for platform, version in platforms.items():
        entries.append(artifact(version, platform + "/work/compat/tacz-port.jar", "TaCZ port"))
        if platform.startswith("fabric-"):
            config_version = "HvR3IdRE" if platform.endswith("1.20.1") else "N5qzq0XV"
            entry = artifact(config_version, platform + "/work/compat/forge-config-api-port.jar", "TaCZ runtime prerequisite")
            entries.append(entry)
    for entry in entries:
        if entry['purpose'] == 'TaCZ port':entry['jarLicense'] = 'GPL3 / CC BY-NC-ND 4.0'
    MANIFEST.write_text(json.dumps({"checked": "2026-10-07", "dependencies": entries}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return entries

def download(entry):
    path = ROOT / entry["path"]
    if path.exists() and hashlib.sha512(path.read_bytes()).hexdigest() == entry["sha512"]:
        return entry["path"] + " verified (cached)"
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".download")
    request = urllib.request.Request(entry["url"], headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=90) as response, temporary.open("wb") as output:
        while chunk := response.read(1024 * 1024):
            output.write(chunk)
    if hashlib.sha512(temporary.read_bytes()).hexdigest() != entry["sha512"]:
        raise ValueError("Dependency hash mismatch: " + entry["path"])
    temporary.replace(path)
    return entry["path"] + " downloaded and verified"

def extract_dev_nested(entry):
    # Loom strips jar-in-jar entries from a locally remapped mod. Expose its own
    # bundled libraries explicitly in development, leaving the release dependency optional.
    if not entry['path'].startswith('fabric-'):return
    path = ROOT / entry['path']
    directory = path.parent / 'nested'
    pending = [path.read_bytes()]
    seen = {}
    while pending:
        with zipfile.ZipFile(io.BytesIO(pending.pop())) as archive:
            if 'fabric.mod.json' not in archive.namelist():continue
            for nested in json.loads(archive.read('fabric.mod.json')).get('jars', []):
                name = nested['file']
                if not name.startswith('META-INF/jars/') or not name.endswith('.jar') or '..' in name.split('/'):
                    raise ValueError('Unexpected nested library path: ' + name)
                basename = name.rsplit('/', 1)[-1]
                data = archive.read(name)
                if basename in seen:
                    if seen[basename] != data:raise ValueError('Conflicting nested library: ' + basename)
                    continue
                seen[basename] = data
                if len(seen)>64:raise ValueError('Too many nested libraries')
                directory.mkdir(exist_ok=True)
                output=directory/basename
                if not output.exists() or output.read_bytes()!=data:output.write_bytes(data)
                pending.append(data)
    print(f'{entry["path"]}: {len(seen)} original nested libraries exposed for Loom development only', flush=True)

if __name__ == "__main__":
    entries = json.loads(MANIFEST.read_text(encoding="utf-8"))["dependencies"] if MANIFEST.exists() else configure()
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        for result in pool.map(download, entries):
            print(result, flush=True)
    for entry in entries:extract_dev_nested(entry)
