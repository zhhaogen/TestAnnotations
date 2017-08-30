package cn.zhg.test.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
public @interface InjectParcel
{
    /**
     * 指定生成类名
     * @return
     */
    String value() default "";
}
