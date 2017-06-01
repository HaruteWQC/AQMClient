package com.wangqc.aqm;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * Created by wang9 on 2017/4/17.
 */
public class PinyinUtil {

    /**
     * 将字符串中的中文转化为拼音,英文字符不变
     *
     * @param inputString 汉语字符串
     * @return
     */
    public static String getPingYin(String inputString) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        String output = "";
        try {
            if (inputString != null && inputString.length() > 0
                    && !"null".equals(inputString)) {
                char[] input = inputString.trim().toCharArray();
                for (int i = 0; i < input.length; i++) {
                    if (Character.toString(input[i]).matches(
                            "[\\u4E00-\\u9FA5]+")) {
                        String[] temp = PinyinHelper.toHanyuPinyinStringArray(
                                input[i], format);
                        output += temp[0];
                    } else {
                        output += Character.toString(input[i]);
                    }
                }
                return output;

            } else {
                return null;
            }
        }catch (Exception e){
            return null;
        }
    }
}