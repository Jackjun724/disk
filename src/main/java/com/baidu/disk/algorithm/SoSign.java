package com.baidu.disk.algorithm;

import com.baidu.disk.config.BaiduYunProperties;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.linux.android.AndroidARMEmulator;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * @author JackJun
 * @date 2020/11/14 11:08 下午
 */
@Component
public class SoSign extends AbstractJni {

    private final BaiduYunProperties baiduYunProperties;

    public SoSign(BaiduYunProperties baiduYunProperties) {
        this.baiduYunProperties = baiduYunProperties;

        //创建进程，使用app本身的进程就可以绕过进程检测
        emulator = new AndroidARMEmulator("com.baidu.netdisk");
        final Memory memory = emulator.getMemory();
        //作者支持19和23两个sdk
        memory.setLibraryResolver(new AndroidResolver(23));

        //创建DalvikVM，利用apk本身，可以为null
        //如果用apk文件加载so的话，会自动处理签名方面的jni，具体可看AbstractJni,这就是利用apk加载的好处
        vm = emulator.createDalvikVM(new File(baiduYunProperties.getApkPath()));
        vm.setJni(this);
        vm.setVerbose(false);
        //加载so，使用armv8-64速度会快很多
        DalvikModule dm = vm.loadLibrary(new File(baiduYunProperties.getSoPath()), false);
        //调用jni
        dm.callJNI_OnLoad(emulator);
        //Jni调用的类，加载so
        dvmClass = vm.resolveClass("com/baidu/netdisk/security/URLHandler");
    }

    //ARM模拟器
    private final AndroidEmulator emulator;

    //vm
    private final VM vm;

    private final DvmClass dvmClass;

    private String sk;

    public String handlerUrl(String url, String sk) {
        return handlerUrl(url, sk, baiduYunProperties.getBduss(), baiduYunProperties.getUid());
    }

    public String handlerUrl(String url, String sk, String bduss, String uid) {
        //获取encodeByte地址
        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(null);
        this.sk = sk;
        StringObject fullUrl = dvmClass.callStaticJniMethodObject(emulator, "handlerURL(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                vm.addLocalObject(context),
                vm.addLocalObject(new StringObject(vm, url)),
                vm.addLocalObject(new StringObject(vm, bduss)),
                vm.addLocalObject(new StringObject(vm, uid)));
        return fullUrl.getValue();
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "com/baidu/netdisk/security/URLHandler->getSK()Ljava/lang/String;":
                return new StringObject(vm, sk);
            case "com/baidu/netdisk/security/URLHandler->getDeviceID()Ljava/lang/String;":
                return new StringObject(vm, baiduYunProperties.getDevUid());
            case "android/content/Context->getPackageName()Ljava/lang/String;": {
                return new StringObject(vm, "com.baidu.netdisk");
            }
        }

        return super.callStaticObjectMethodV(vm, dvmObject, signature, vaList);
    }


    /**
     * ---- x500 x509 resolve ----
     **/

    private X509Certificate x509Certificate = null;

    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("javax/security/auth/x500/X500Principal-><init>(Ljava/lang/String;)V".equals(signature)) {
            StringObject stringObject = vaList.getObject(0);
            assert stringObject != null;
            return vm.resolveClass("javax/security/auth/x500/X500Principal").newObject(new X500Principal(stringObject.getValue()));
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if ("javax/security/auth/x500/X500Principal->equals(Ljava/lang/Object;)Z".equals(signature)) {
            return true;
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "javax/security/auth/x500/X500Principal->toString()Ljava/lang/String;": {
                X500Principal x500Principal = (X500Principal) dvmObject.getValue();
                return new StringObject(vm, x500Principal.toString());
            }
            case "android/content/Context->getPackageName()Ljava/lang/String;": {
                return new StringObject(vm, "com.baidu.netdisk");
            }
            case "java/security/cert/CertificateFactory->generateCertificate(Ljava/io/InputStream;)Ljava/security/cert/Certificate;": {
                CertificateFactory factory = (CertificateFactory) dvmObject.getValue();
                DvmObject<?> stream = vaList.getObject(0);
                assert stream != null;
                InputStream inputStream = (InputStream) stream.getValue();
                try {
                    x509Certificate = (X509Certificate) factory.generateCertificate(inputStream);
                    return vm.resolveClass("java/security/cert/X509Certificate").newObject(x509Certificate);
                } catch (CertificateException e) {
                    throw new IllegalStateException(e);
                }
            }
            case "java/security/cert/X509Certificate->getSubjectX500Principal()Ljavax/security/auth/x500/X500Principal;": {
                return vm.resolveClass("javax/security/auth/x500/X500Principal").newObject(x509Certificate.getSubjectX500Principal());
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @PreDestroy
    void destroy() throws IOException {
        emulator.close();
    }
}
