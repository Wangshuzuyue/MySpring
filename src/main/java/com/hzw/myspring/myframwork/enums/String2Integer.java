package com.hzw.myspring.myframwork.enums;

/**
 * @Auther: huangzuwang
 * @Date: 2019/3/26 16:48
 * @Description:
 */
public class String2Integer implements TypeConverter<String, Integer> {
    @Override
    public Integer convert(String value) {
        return Integer.valueOf(value);
    }
}
