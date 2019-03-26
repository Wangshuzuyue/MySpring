package com.hzw.myspring.myframwork.enums;

/**
 * @Auther: huangzuwang
 * @Date: 2019/3/26 16:45
 * @Description:
 */
public enum TypeConvertEnum {
    STRING2INTEGER(Integer.class, new String2Integer()),
    STRING2DOUBLE(Double.class, new String2Double()),
    STRING2LONG(Long.class, new String2Long()),

    ;

    /**
     * 目标类型
     */
    private Class clazz;

    /**
     * 类型转换器
     */
    private TypeConverter typeConverter;

    TypeConvertEnum(Class clazz, TypeConverter typeConverter) {
        this.clazz = clazz;
        this.typeConverter = typeConverter;
    }

    public static TypeConvertEnum getByClazz(Class clazz){
        for (TypeConvertEnum typeConvertEnum : TypeConvertEnum.values()){
            if (typeConvertEnum.clazz == clazz){
                return typeConvertEnum;
            }
        }
        return null;
    }

    public TypeConverter getTypeConverter(){
        return this.typeConverter;
    }
}
