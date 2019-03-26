package com.hzw.myspring.myframwork.enums;

/**
 * @Auther: huangzuwang
 * @Date: 2019/3/26 16:48
 * @Description:
 */
public class String2Long implements TypeConverter<String, Long> {
    @Override
    public Long convert(String value) {
        return Long.valueOf(value);
    }
}
