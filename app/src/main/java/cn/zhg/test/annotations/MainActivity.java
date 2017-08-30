package cn.zhg.test.annotations;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.logging.Logger;

public class MainActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        User u=new User();
        u.name="xiao xiao";
        u.age=21;
        Intent intent=new Intent();
        intent.putExtra("user",u);//传递数据
        Log.d("test",intent.getExtras()+"");
    }
}
