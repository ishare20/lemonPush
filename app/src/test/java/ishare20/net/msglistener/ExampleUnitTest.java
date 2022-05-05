package ishare20.net.msglistener;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        String text="【华为】验证码725019，用于华为帐号157******45登录。转给他人将导致帐号被盗和个人信息泄露，谨防诈骗。如非您操作请忽略。";
       // String text="验证2114中文";
        String pattern="[0-9]{4,6}";
        Pattern p=Pattern.compile(pattern);
        Matcher m=p.matcher(text);
        if (m.find()){
            System.out.println(m.group());
        }

    }
}