package jsonhelper;

public class ExceptionReturn {
    public String exceptionType;
    public String exceptionInfo;

    public ExceptionReturn(String exception_type, String exception_info) {
        this.exceptionType = exception_type;
        this.exceptionInfo = exception_info;
    }
    
    @Override
    public String toString() {
        return "ExceptionReturn: " + "exception_type = " + exceptionType + " exception_info = " + exceptionInfo;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ExceptionReturn)) return false;
        ExceptionReturn exceptionReturn = (ExceptionReturn) obj;
        return this.exceptionType.equals(exceptionReturn.exceptionType) && this.exceptionInfo.equals(exceptionReturn.exceptionInfo);
    }
}
