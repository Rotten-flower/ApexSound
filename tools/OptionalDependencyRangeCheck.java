import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

/** Run with Maven Artifact and Fabric Loader on the classpath, using the range from the built JAR. */
public final class OptionalDependencyRangeCheck {
    public static void main(String[] args) throws Exception {
        String[] versions={"1.1.8-hotfix","1.1.8-hotfix2","1.1.8-hotfix-r7","0.7.1-forge1.1.8-hotfix2","99.0.0"};
        if(args[0].equals("maven")) {
            var empty=VersionRange.createFromVersionSpec("");
            if(empty.containsVersion(new DefaultArtifactVersion("1.1.8-hotfix2")))
                throw new AssertionError("Regression setup no longer reproduces the omitted-range failure");
            var range=VersionRange.createFromVersionSpec(args[1]);
            for(String version:versions)if(!range.containsVersion(new DefaultArtifactVersion(version)))
                throw new AssertionError("Optional dependency rejected "+version);
        } else {
            var range=VersionPredicate.parse(args[1]);
            for(String version:versions)if(!range.test(Version.parse(version)))
                throw new AssertionError("Optional dependency rejected "+version);
        }
        System.out.println("PASS "+args[0]+" optional range "+args[1]+" accepts "+versions.length+" installed versions");
    }
}
