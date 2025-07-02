package com.example.fido_test_simple;


import android.content.DialogInterface;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private KeyStore keyStore;
    private Cipher cipher;
    private SecretKey secretKey;
    private String alias = "fido_test";

    EditText userid = null;
    TextView textView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userid = (EditText) findViewById(R.id.login_id);
        textView = findViewById(R.id.textView);

        executor = ContextCompat.getMainExecutor(this);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationSucceeded(
                        @NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);

                    // 생체인증 성공 후 암호화
                    // doFinal : 생체 인증된 사용자만 이 키를 사용해 'test1234'를 암호화한다.
                    byte[] encryptedInfo = new byte[0];
                    try {
                        final String strID = userid.getText().toString().trim();

                        encryptedInfo = result.getCryptoObject().getCipher().doFinal(strID.getBytes(Charset.defaultCharset()));
                    } catch (BadPaddingException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalBlockSizeException e) {
                        throw new RuntimeException(e);
                    }

                    Log.d("MY_APP_TAG", "Encrypted information: " + Arrays.toString(encryptedInfo));
                    textView.setText(Arrays.toString(encryptedInfo));

                    Toast.makeText(getApplicationContext(), "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            });
        }

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("생체인증 로그인")
                .setSubtitle("생체인증으로 로그인합니다.")
                .setNegativeButtonText("비밀번호로 로그인하기")
                .build();

        Button biometricLoginButton = findViewById(R.id.biometric_login);
        biometricLoginButton.setOnClickListener(view -> {

            cipher = getCipher();
            secretKey = getSecretKey();

            if (cipher == null) {
                Toast.makeText(getApplicationContext(), "cipher 암호화 초기화 실패", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (secretKey == null) {
                Toast.makeText(getApplicationContext(), "secretKey 암호화 초기화 실패", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                biometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(cipher));
            } catch (InvalidKeyException e) {
                Log.e("JYEJON", "Cipher init failed: " + e.getMessage());
                //Toast.makeText(this, "생체인증이 추가 되었습니다.", Toast.LENGTH_SHORT).show();

                AlertDialog.Builder menu = new AlertDialog.Builder(MainActivity.this);
                menu.setIcon(R.mipmap.ic_launcher);
                menu.setTitle("생체인증 테스트");
                menu.setMessage("생체인증에 변화가 생겼습니다. 재등록 하시겠습니까?");

                menu.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        try {
                            keyStore.deleteEntry(alias);
                        } catch (Exception exception) {
                            exception.getMessage();
                        }

                        // dialog 제거
                        dialog.dismiss();
                    }
                });

                menu.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // dialog 제거
                        dialog.dismiss();
                    }
                });

                menu.show();

            }
        });


    }

    private void generateSecretKey(KeyGenParameterSpec keyGenParameterSpec) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();

            Log.d("JYEJON", "키 생성 완료");

        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            Log.e("JYEJON", "알고리즘 사용 못함 : " + noSuchAlgorithmException.getMessage());
            Toast.makeText(this, "알고리즘 사용 못함 : " + noSuchAlgorithmException.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (NoSuchProviderException NoSuchProviderException) {
            Log.e("JYEJON", "키스토어 파일 못찾음 : " + NoSuchProviderException.getMessage());
            Toast.makeText(this, "키스토어 파일 못찾음 : " + NoSuchProviderException.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (InvalidAlgorithmParameterException InvalidAlgorithmParameterException) {
            Log.e("JYEJON", "지문이 등록되지 않음 : " + InvalidAlgorithmParameterException.getMessage());
            Toast.makeText(this, "지문이 등록되지 않음 : " + InvalidAlgorithmParameterException.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("JYEJON", e.getMessage());
        }

    }

    private SecretKey getSecretKey() {
        // 키가 없다면 생성
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (!keyStore.containsAlias(alias)) {
                generateSecretKey(new KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(true)
                        .setInvalidatedByBiometricEnrollment(true)  // 생체 정보를 추가/삭제하면 이 키는 무효화 된다.
                        .build());
                Log.d("JYEJON", "키 생성 완료");
            } else {
                Log.d("JYEJON", "키 이미 존재");
            }

            // 키가 정상적으로 등록 되었는지 확인
            Key key = keyStore.getKey(alias, null);
            Log.d("JYEJON", "key class: " + (key != null ? key.getClass().getName() : "null"));

            return ((SecretKey) keyStore.getKey(alias, null));
        } catch (Exception e) {
            Log.e("JYEJON", "키 생성 중 오류: " + e.toString());

            Log.e("JYEJON", "키 생성 중 오류 class: " + e.getClass());
            return null;
        }
    }

    private Cipher getCipher() {
        try {
            return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (Exception e) {
            Log.e("JYEJON", e.getMessage());
            return null;
        }
    }
}