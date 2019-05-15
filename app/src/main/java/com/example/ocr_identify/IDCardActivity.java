package com.example.ocr_identify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.IDCardParams;
import com.baidu.ocr.sdk.model.IDCardResult;
import com.example.ocr_identify.utils.FileUtils;
import com.example.ocr_identify.utils.ToastUtils;
import com.example.ocr_ui.camera.CameraActivity;

import java.io.File;

/**
 * 身份证识别
 *
 * 身份证自动识别,银行卡识别,驾驶证识别,行驶证识别,根据百度文字识别 api 封装,
 * 能快速识别身份证信息,银行卡信息,驾驶证信息,行驶证信息,使用非常方便
 * https://github.com/wenchaosong/OCR_identify
 *
 应用名称
 AppID
 API Key
 Secret Key

 ocrIdentify
 16255231
 qjzb3NvOupsQrLYFdAQ2MLtY
 ad6IomGbomQGZhaPU1jz4VWMnTgmtWW1
 *
 *
 */
public class IDCardActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "IDCardActivity";
    private static final int REQUEST_CODE_CAMERA = 102; //照相机扫描的请求码

    private TextView resultTv; //扫描读取的结果
    private TextView nameTv; //姓名
    private TextView idNumberTv; //身份证号码
    private TextView effectiveDateTv; //有效日期
    private Context mContext;
    private int idType; //身份证类型，0：正面，1：反面

    private static final String API_KEY = "qjzb3NvOupsQrLYFdAQ2MLtY";
    private static final String SECRET_KEY = "ad6IomGbomQGZhaPU1jz4VWMnTgmtWW1";
    //百度AI开放平台使用OAuth2.0授权调用开放API，调用API时必须在URL中带上accesss_token参数。AccessToken可用AK/SK或者授权文件的方式获得。
    private boolean hasGotToken = false; //是否已经获取到了Token



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idcard);

        mContext = this;
        initView();

        // 请选择您的初始化方式
        initAccessToken();  //授权文件、安全模式
//        initAccessTokenWithAkSk();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放内存资源
        OCR.getInstance(this).release();
    }

    //授权文件（安全模式）
    //此种身份验证方案使用授权文件获得AccessToken，缓存在本地。建议有安全考虑的开发者使用此种身份验证方式。
    private void initAccessToken() {
        OCR.getInstance(this).initAccessToken(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken accessToken) {
                // 调用成功，返回AccessToken对象
                String token = accessToken.getAccessToken();
                Log.i(TAG, "token:-------->" + token);
                hasGotToken = true;
            }

            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
                Log.i(TAG, "onError:licence方式获取token失败---->" + error.getMessage());
                ToastUtils.showToast(mContext, "licence方式获取token失败  " + error.getMessage());
            }
        }, getApplicationContext());
    }

    //通过AK/SK的方式获得AccessToken。
    private void initAccessTokenWithAkSk() {
        OCR.getInstance(this).initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken result) {
                // 调用成功，返回AccessToken对象
                String token = result.getAccessToken();
                Log.i(TAG, "token:-------->" + token);
                hasGotToken = true;
            }

            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
                Log.i(TAG, "onError:AK，SK方式获取token失败---->" + error.getMessage());
                ToastUtils.showToast(mContext, "AK，SK方式获取token失败" + error.getMessage());
            }
        }, getApplicationContext(), API_KEY, SECRET_KEY);
    }




    //初始化View
    private void initView() {
        findViewById(R.id.id_card_front_btn).setOnClickListener(this);
        findViewById(R.id.id_card_back_btn).setOnClickListener(this);
        resultTv = (TextView) findViewById(R.id.info_text_view);
        nameTv = (TextView) findViewById(R.id.name_tv);
        idNumberTv = (TextView) findViewById(R.id.id_number_tv);
        effectiveDateTv = (TextView) findViewById(R.id.effective_date_tv);
    }

    // 调用拍摄身份证正面（不带本地质量控制）activity
    private void scanFront() {
        Intent intent = new Intent(this, CameraActivity.class);
        // 设置临时存储
        intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH,
                FileUtils.getSaveFile(getApplication()).getAbsolutePath());
        //设置扫描的身份证的类型（正面front还是反面back）
        intent.putExtra(CameraActivity.KEY_CONTENT_TYPE, CameraActivity.CONTENT_TYPE_ID_CARD_FRONT);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }


    //调用拍摄身份证反面（不带本地质量控制）activity
    private void scanBack() {
        Intent intent = new Intent(IDCardActivity.this, CameraActivity.class);
        intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH,
                FileUtils.getSaveFile(getApplication()).getAbsolutePath());
        intent.putExtra(CameraActivity.KEY_CONTENT_TYPE, CameraActivity.CONTENT_TYPE_ID_CARD_BACK);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }


    /**
     * 识别身份证
     *
     * @param idCardSide 正面（front）还是反面（back）
     * @param filePath   文件路径
     */
    private void recIDCard(String idCardSide, String filePath) {
        IDCardParams param = new IDCardParams();
        param.setImageFile(new File(filePath));
        // 设置身份证正反面
        param.setIdCardSide(idCardSide);
        // 设置方向检测
        param.setDetectDirection(true);
        // 设置图像参数压缩质量0-100, 越大图像质量越好但是请求时间越长。 不设置则默认值为20
        param.setImageQuality(20);
        // 调用身份证识别服务
        OCR.getInstance(this).recognizeIDCard(param, new OnResultListener<IDCardResult>() {
            @Override
            public void onResult(IDCardResult result) {
                // 调用成功，返回IDCardResult对象
                if (result != null) {
                    resultTv.setText(result.toString());
                    Log.i(TAG, "result: " + result.toString());
                    if (idType == 0) {
                        //正面
                        String name = result.getName().toString(); //姓名
                        String gender = result.getGender().toString(); //性别
                        String ethnic = result.getEthnic().toString(); //民族
                        String birthday = result.getBirthday().toString(); //出生日期
                        String address = result.getAddress().toString(); //居住地址
                        String idNumber = result.getIdNumber().toString(); //身份证号码
                        Log.i(TAG, "name:----------->" + name);
                        Log.i(TAG, "gender:----------->" + gender);
                        Log.i(TAG, "ethnic:----------->" + ethnic);
                        Log.i(TAG, "birthday:----------->" + birthday);
                        Log.i(TAG, "address:----------->" + address);
                        Log.i(TAG, "idNumber:----------->" + idNumber);
                        nameTv.setText(name);
                        idNumberTv.setText(idNumber);
                    } else {
                        //反面
                        String signDate = result.getSignDate().toString(); //签发日期
                        String expiryDate = result.getExpiryDate().toString(); //截止日期
                        String issueAuthority = result.getIssueAuthority().toString();//签发机关 
                        Log.i(TAG, "signDate:----------->" + signDate);
                        Log.i(TAG, "expiryDate:----------->" + expiryDate);
                        Log.i(TAG, "issueAuthority:----------->" + issueAuthority);
                        effectiveDateTv.setText(signDate + "~" + expiryDate);
                    }
                }
            }

            @Override
            public void onError(OCRError error) {
                // 调用失败，返回OCRError对象
                Log.i(TAG, "onError: " + error.getMessage());
                ToastUtils.showToast(mContext, error.getMessage());
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_card_front_btn:
                //身份证正面
                idType = 0;
                scanFront();
                break;
            case R.id.id_card_back_btn:
                //身份证反面
                idType = 1;
                scanBack();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //如果拍摄类型是身份证
        if (requestCode == REQUEST_CODE_CAMERA && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String contentType = data.getStringExtra(CameraActivity.KEY_CONTENT_TYPE);
                String filePath = FileUtils.getSaveFile(getApplicationContext()).getAbsolutePath();
                if (!TextUtils.isEmpty(contentType)) {
                    //判断是身份证正面还是反面
                    if (CameraActivity.CONTENT_TYPE_ID_CARD_FRONT.equals(contentType)) {
                        recIDCard(IDCardParams.ID_CARD_SIDE_FRONT, filePath);
                    } else if (CameraActivity.CONTENT_TYPE_ID_CARD_BACK.equals(contentType)) {
                        recIDCard(IDCardParams.ID_CARD_SIDE_BACK, filePath);
                    }
                }
            }
        }
    }
}
