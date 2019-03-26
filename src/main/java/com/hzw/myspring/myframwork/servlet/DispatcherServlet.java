package com.hzw.myspring.myframwork.servlet;

import com.hzw.myspring.myframwork.annotation.*;
import com.hzw.myspring.myframwork.enums.TypeConvertEnum;
import com.hzw.myspring.myframwork.enums.TypeConverter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Auther: huangzuwang
 * @Date: 2019/3/25 11:23
 * @Description:
 */
public class DispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    private static final String LOCATION_KEY = "springConfigLocation";

    private static final String SCAN_PACKAGE_KEY = "scan.package";

    private List<String> classNames = new ArrayList<String>();

    private Map<String,Object> ioc = new HashMap<String,Object>();

    //保存所有的Url和方法的映射关系
    private List<Handler> handlerMapping = new ArrayList<Handler>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件
        loadConfig(config.getInitParameter(LOCATION_KEY));

        //2.扫描类路径下所有的类
        scanPackage(properties.getProperty(SCAN_PACKAGE_KEY));

        //3.初始化类对象，保存到IOC容器中
        instanceBean();

        //4.依赖注入
        autoWired();

        //5.建立url和controller.method关系映射 handlerMapping
        createHandlerMapping();

        //6.请求
    }

    private void loadConfig(String configLocation) {
        InputStream fis = null;
        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(configLocation);
            //1、读取配置文件,放入Properties对象中
            properties.load(fis);
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            try {
                if(null != fis){fis.close();}
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void scanPackage(String packageName) {
        //将所有的包路径转换为文件路径
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            //文件夹，递归
            if(file.isDirectory()){
                scanPackage(packageName + "." + file.getName());
                //文件名后缀判断
            }else if(file.getName().endsWith(".class")){
                classNames.add(packageName + "." + file.getName().replace(".class", "").trim());
            }
        }
    }

    private String toLowerCaseFirstOne(String s) {
        if(Character.isLowerCase(s.charAt(0))){
            return s;
        }
        else{
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
        }
    }

    private void instanceBean() {
        if(classNames.size() == 0){ return; }
        try{
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                //判断类上是否有MyController注解
                if(clazz.isAnnotationPresent(MyController.class)){
                    //将首字母小写作为beanName
                    String beanName = toLowerCaseFirstOne(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                }else if(clazz.isAnnotationPresent(MyService.class)){

                    MyService service = clazz.getAnnotation(MyService.class);
                    String beanName = service.value();
                    //用户自定义名字
                    if(!"".equals(beanName.trim())){
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    }
                    //按接口类型创建一个实例
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        ioc.put(i.getName(), clazz.newInstance());
                    }

                }else{
                    continue;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void autoWired() {
        if(ioc.isEmpty()){return;}
        //遍历已实例化存入IOC的Bean
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //Declared 所有的，特定的 字段，包括private/protected/default
            //正常来说，普通的OOP编程只能拿到public的属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if(!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);

                //如果用户没有自定义beanName，默认就根据类型注入
                //这个地方省去了对类名首字母小写的情况的判断，这个作为课后作业
                //小伙伴们自己去完善
                String beanName = autowired.value().trim();
                if("".equals(beanName)){
                    //获得接口的类型，作为key待会拿这个key到ioc容器中去取值
                    beanName = field.getType().getName();
                }

                //如果是public以外的修饰符，只要加了@Autowired注解，都要强制赋值
                //反射中叫做暴力访问， 强吻
                field.setAccessible(true);

                try {
                    //用反射机制，动态给字段赋值
                    //参数: 1.遍历中的Bean 2.注入到字段上的Bean
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void createHandlerMapping() {
        if(ioc.isEmpty()){ return; }
        //遍历所有ioc中的Bean
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            if(!clazz.isAnnotationPresent(MyController.class)){continue;}


            //保存写在类上面的@GPRequestMapping("/demo")
            String baseUrl = "";
            if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //默认遍历所有的public方法
            for (Method method : clazz.getMethods()) {
                if(!method.isAnnotationPresent(MyRequestMapping.class)){continue;}

                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                //优化
                // //demo///query, 去掉重复的"/"
                String regex = ("/" + baseUrl + "/" + requestMapping.value())
                        .replaceAll("/+","/");
                Pattern pattern = Pattern.compile(regex);
                this.handlerMapping.add(new Handler(pattern,entry.getValue(),method));
                System.out.println("Mapped :" + pattern + "," + method);

            }
        }
    }

    /**
     * 保存一个url和一个Method的关系
     */
    public class Handler {
        /**
         * 匹配url用的正则
         */
        private Pattern pattern;
        private Method method;
        private Object controller;
        /**
         * 方法的参数类型数组
         */
        private Class<?> [] paramTypes;

        public Pattern getPattern() {
            return pattern;
        }

        public Method getMethod() {
            return method;
        }

        public Object getController() {
            return controller;
        }

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }

        /**
         * 形参列表
         * 参数的名字作为key,参数的顺序，位置作为值
         */
        private Map<String,Integer> paramIndexMapping;

        public Handler(Pattern pattern, Object controller, Method method) {
            this.pattern = pattern;
            this.method = method;
            this.controller = controller;

            paramTypes = method.getParameterTypes();

            paramIndexMapping = new HashMap<String, Integer>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method){

            //提取方法中加了注解的参数
            //把方法上的注解拿到，得到的是一个二维数组
            //因为一个参数可以有多个注解，而一个方法又有多个参数
            Annotation [] [] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length ; i ++) {
                for(Annotation a : pa[i]){
                    if(a instanceof MyRequestParam){
                        String paramName = ((MyRequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }

            //提取方法中的request和response参数
            Class<?> [] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length ; i ++) {
                Class<?> type = paramsTypes[i];
                if(type == HttpServletRequest.class ||
                        type == HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(),i);
                }
            }

        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try{
            //开始始匹配到对应的方方法
            doDispatch(req,resp);

        }catch(Exception e){
            e.printStackTrace();
            //如果匹配过程出现异常，将异常信息打印出去
            resp.getWriter().write("500 Exception,Details:\r\n" + Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]", "").replaceAll(",\\s", "\r\n"));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        Handler handler = getHandler(req);
        if(handler == null){
//        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!!");
            return;
        }

        //获得方法的形参列表
        Class<?> [] paramTypes = handler.getParamTypes();

        Object [] paramValues = new Object[paramTypes.length];

        Map<String,String[]> params = req.getParameterMap();
        for (Map.Entry<String, String[]> parm : params.entrySet()) {
            String value = Arrays.toString(parm.getValue()).replaceAll("\\[|\\]","")
                    .replaceAll("\\s",",");

            if(!handler.paramIndexMapping.containsKey(parm.getKey())){continue;}

            int index = handler.paramIndexMapping.get(parm.getKey());
            //类型不一样才需要转换
            if (paramTypes[index] != value.getClass()){
                paramValues[index] = convert(paramTypes[index],value);
            }else{
                paramValues[index] = value;
            }

        }

        if(handler.paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
        }

        if(handler.paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;
        }

        Object returnValue = handler.method.invoke(handler.controller,paramValues);
        if(returnValue == null || returnValue instanceof Void){ return; }
        resp.getWriter().write(returnValue.toString());
    }

    private Handler getHandler(HttpServletRequest req) throws Exception{
        if(handlerMapping.isEmpty()){return null;}
        //绝对路径
        String url = req.getRequestURI();
        //处理成相对路径
        String contextPath = req.getContextPath();
        //处理重复的斜杠
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");

        for (Handler handler : this.handlerMapping) {
            Matcher matcher = handler.getPattern().matcher(url);
            if(!matcher.matches()){ continue;}
            return handler;
        }
        return null;
    }

    private Object convert(Class<?> type,String value){
        TypeConvertEnum typeConvertEnum = TypeConvertEnum.getByClazz(type);
        //策略+单例模式类型转换器实现
        TypeConverter typeConverter = typeConvertEnum.getTypeConverter();
        return typeConverter.convert(value);
    }
}
