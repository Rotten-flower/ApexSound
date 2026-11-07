import dev.flower.apex_sound_effects.core.CumulativeDamage;

public final class CumulativeDamageCheck {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args) {
        var stack=new CumulativeDamage();
        check(CumulativeDamage.format(stack.add(1,6.4f,0,false)).equals("6"),"first hit");
        check(CumulativeDamage.format(stack.add(1,7.4f,100_000_000L,false)).equals("14"),"sum before rounding");
        check(CumulativeDamage.format(stack.add(2,4.4f,200_000_000L,false)).equals("4"),"separate target");
        check(CumulativeDamage.format(stack.add(1,2.4f,300_000_000L,false)).equals("16"),"return to target");
        check(CumulativeDamage.format(stack.add(1,3.4f,400_000_000L,false)).equals("20"),"armor to flesh continues");
        check(CumulativeDamage.format(stack.add(1,1,1_399_999_999L,false)).equals("21"),"sliding window before boundary");
        check(CumulativeDamage.format(stack.add(1,2,2_399_999_999L,false)).equals("2"),"exact one second expires");
        check(CumulativeDamage.format(stack.add(1,3,2_500_000_000L,true)).equals("5"),"terminal displays final total");
        check(CumulativeDamage.format(stack.add(1,4,2_600_000_000L,false)).equals("4"),"terminal clears target");
        stack.clear();
        check(stack.add(1,2,2_700_000_000L,false)==2,"world change clears");
        check(stack.add(1,Float.NaN,2_800_000_000L,false)==0,"NaN rejected");
        check(stack.add(1,Float.POSITIVE_INFINITY,2_800_000_000L,false)==0,"infinite rejected");
        check(stack.add(1,-1,2_800_000_000L,false)==0,"negative rejected");
        check(stack.add(1,0,2_800_000_000L,false)==0,"zero rejected");
        check(stack.add(1,2,2_900_000_000L,false)==4,"invalid hits leave total intact");
        stack.clear();
        for(int i=0;i<65;i++)stack.add(i,1,3_000_000_000L,false);
        check(stack.add(0,1,3_100_000_000L,false)==1,"bounded cache evicts oldest");
        check(stack.add(64,1,3_100_000_000L,false)==2,"newest target retained");
        check(CumulativeDamage.format(.5).equals("1"),"round half up");
        System.out.println("PASS: "+checks+" damage stacking and reset checks");
    }
}
