package com.hzw.myspring.myframwork.enums;

/**
 * @Auther: huangzuwang
 * @Date: 2019/3/26 16:48
 * @Description:
 */
public class String2Double implements TypeConverter<String, Double> {
    @Override
    public Double convert(String value) {
        return Double.valueOf(value);
    }
}
