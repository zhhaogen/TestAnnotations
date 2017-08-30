package cn.zhg.test.processor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import cn.zhg.test.annotations.*;

import static javax.tools.StandardLocation.*;

@SupportedAnnotationTypes("cn.zhg.test.annotations.*")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TestInjectProcessor extends AbstractProcessor
{
    private Elements elementUtils;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment)
    {
        super.init(processingEnvironment);
        elementUtils=processingEnvironment.getElementUtils();
        log("Options = "+processingEnvironment.getOptions());
        log("SourceVersion = "+processingEnvironment.getSourceVersion());
        log("SupportedAnnotationTypes = "+this.getSupportedAnnotationTypes());
        log("SupportedOptions = "+this.getSupportedOptions());
    }

    private void log(String msg)
    {
       // this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,msg);
        try(FileWriter wr=new FileWriter("processor.log",true))
        {
            wr.write(msg+"\n");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment)
    {
        log("正在处理...");
        Set<? extends Element> annotations = roundEnvironment.getElementsAnnotatedWith(InjectParcel.class);
        if(annotations!=null )
        {
            for(Element ele:annotations)
            {
                if(ele.getKind()== ElementKind.CLASS)
                {
                    TypeElement classElement = (TypeElement) ele;
                    String packageName = elementUtils.getPackageOf(classElement).getQualifiedName().toString();//包名
                    String  className = classElement.getQualifiedName().toString();//全名
                    String  simpleName = classElement.getSimpleName().toString();//简名
                    String targetClassName=simpleName+"Parcel";//生成类的名称
                    InjectParcel an = classElement.getAnnotation(InjectParcel.class);
                    if(!an.value().isEmpty())
                    {
                        targetClassName=an.value();
                    }
                    try
                    {
                        List<VariableElement> eles = new ArrayList<VariableElement>();
                        getClassFieldElements(classElement, eles);
                        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(packageName+"."+targetClassName);
                      String END="\n";
                       try( Writer writer = jfo.openWriter())
                       {
                           //生成源代码
                           StringBuffer builder=new StringBuffer();
                           builder.append("//自动生成于"+new Date().toLocaleString()).append(END);//注释
                           builder.append("package "+packageName).append(";").append(END);//包名
                           builder.append("import android.os.Parcel;").append(END);//导入
                           builder.append("import android.os.Parcelable;").append(END);
                           builder.append("class "+targetClassName).append(" extends ").append(className).append(END);//类名
                           builder.append("implements android.os.Parcelable " ) .append(END);//类名
                           builder.append("{").append(END);//

                           builder.append("\t").append("public   int describeContents()").append(END).append("{").append(END);
                           builder.append("\t").append("return 0;").append(END);
                           builder.append("}").append(END);
                           builder.append("\t").append("public void writeToParcel(Parcel dest, int flags)").append(END).append("{").append(END);
                           builder.append("\t").append("").append(END);
                           for (VariableElement e : eles)
                           {
                               builder.append("\t").append("dest.writeValue(" + e + ");").append(END);
                           }
                           builder.append(END);
                           builder.append("}").append(END);

                           builder.append("\t").append("public static final Parcelable.Creator<"+targetClassName+"> CREATOR = new Parcelable.Creator<"+targetClassName+">(){").append(END);
                           builder.append("\t\t").append("public "+targetClassName+" createFromParcel(Parcel source){").append(END);
                           builder.append("\t\t").append(targetClassName+" obj=new "+targetClassName+"();").append(END);
                           for (VariableElement e : eles)
                           {
                               builder.append("\t\t").append("obj." + e + "=("+this.getParcelMethodType(e.asType())+")source.readValue(null);")
                                       .append(END);
                           }
                           builder.append("\t\t").append("return obj;").append(END);
                           builder.append("}").append(END);
                           builder.append("\t\t").append("public "+targetClassName+"[] newArray(int size){").append(END);
                           builder.append("\t\t").append("return new "+targetClassName+"[size];").append(END);
                           builder.append("}").append(END);
                           builder.append("};").append(END);

                           builder.append("}").append(END);//
                           writer.write(builder.toString());
                       }
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }
    /**
     *
     * @param classElement
     * @param eles
     */
    private void getClassFieldElements(TypeElement classElement, List<VariableElement> eles)
    {
        List<? extends Element> es = classElement.getEnclosedElements();
        for (Element e : es)
        {
            Set<Modifier> mds = e.getModifiers();
            if (e.getKind() == ElementKind.FIELD && mds.contains(Modifier.PUBLIC)
                    && !mds.contains(Modifier.STATIC))
            {
                eles.add((VariableElement) e);
            }
        }
        TypeMirror sup = classElement.getSuperclass();
        if (sup.getKind() != TypeKind.NONE)
        {
            getClassFieldElements((TypeElement) ((DeclaredType) sup).asElement(), eles);
        }
    }
    private String getParcelMethodType(TypeMirror type)
    {
        String cl=type.toString();
        if("boolean".equals(cl))
        {
            return "Boolean";
        }
        if("byte".equals(cl))
        {
            return "Byte";
        }
        if("short".equals(cl))
        {
            return "Short";
        }
        if("char".equals(cl))
        {
            return "Character";
        }
        if("int".equals(cl))
        {
            return "Integer";
        }
        if("float".equals(cl))
        {
            return "Float";
        }
        if("double".equals(cl))
        {
            return "Double";
        }

        return cl;
    }
}
