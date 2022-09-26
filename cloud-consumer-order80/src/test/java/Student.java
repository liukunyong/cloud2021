public class Student {
    /*必填*/
    private String name;
    private int age;
    /*选填*/
    private String sex;
    private String grade;

    public Student(){}
    public Student(String name){
        this();
        this.name = name;
    }
    public Student(String name,int age1){
        this(name);
        age = age1;
        this.toString();
    }
    public String toString(){
        return  this.age+this.name+this.sex+this.grade;
    }
    public static class Builder {
        private String name;
        private int age;
        private String sex = "";
        private String grade = "";

        public Builder(String name, int age) {
            this.name = name;
            this.age = age;
        }
        public Builder sex(String sex) {
            this.sex = sex;
            return this;
        }
        public Builder grade(String grade) {
            this.grade = grade;
            return this;
        }
        public Student build() {
            return new Student(this);
        }
    }
    private Student(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
        this.sex = builder.sex;
        this.grade = builder.grade;
    }
}
