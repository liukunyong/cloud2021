class Person {
    public void eat(shuiguo apple){
        shuiguo peeled = apple.getPeeled();
        System.out.println("Yummy");
    }
}
class Peeler{
    static shuiguo peel(shuiguo apple){
        //....remove peel
        return apple;
    }
}
class Apple extends shuiguo{

}
class Peer extends shuiguo{

}
class shuiguo{
    shuiguo getPeeled(){
        return Peeler.peel(this);
    }
}
class This{
    public static void main(String args[]){
        new Person().eat(new Apple());
        new Person().eat(new Peer());
    }
}
