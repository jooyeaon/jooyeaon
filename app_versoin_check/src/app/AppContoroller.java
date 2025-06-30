package app;

@Controller
@RequestMapping("/app")
public class AppContoroller {

    @Value( "#{ config [ 'app.android.version'   ] }" ) 
    private String android_version;
    
    final static Logger logger = LoggerFactory.getLogger(AppContoroller.class);
	
    final String MSG_STATUS_ANDROID_VERION	= "해당 버전의 앱은 사용 할 수 없습니다.";
 
 
    @RequestMapping(value = "/device_register", method = RequestMethod.POST)
    @ResponseBody
    public String device_register( @RequestParam ( value="data", required=true ) String postData,
                            @RequestParam ( value="key",  required=true ) String key,
                            HttpServletRequest request) throws Exception
    {
        ObjectMapper mapper          = new ObjectMapper();
        Map<String, Object> modelMap = new HashMap<String, Object>();
        JSONObject json 			 = null;


        try {
            json = JsonUtil.stringToJSONObject( cipherService.SimpleAESDecode( postData, key ) );
            
            String OS = json.getString("OS");
            String InstalledAppVer = json.getString("InstalledAppVer");
            int configVersoin = Integer.parseInt(android_version.replaceAll("\\.", ""));
            int installVersion = Integer.parseInt(InstalledAppVer.replaceAll("\\.", ""));
            
            if (OS.equals("Android") && Integer.compare(configVersoin, installVersion) >= 0) {
        		logger.info("device_register>> ERROR>> 사용 할 수 없는 앱 버전" );
				            	
            	//사용 가능한 ID가 아니면
            	modelMap.put("Status", "ERROR");
            	modelMap.put("Message", MSG_STATUS_ANDROID_VERION);
            }

        } catch (Exception e) {
            logger.info("device_register>> catch-Exception>> " + e.toString());
        }
        
        ObjectMapper finalMapper          = new ObjectMapper();
        Map<String, Object> finalModelMap = new HashMap<String, Object>();
        finalModelMap.put( "Data",cipherService.SimpleAESEncode(mapper.writeValueAsString(modelMap),key) );
        finalModelMap.put( "key",key );
        return finalMapper.writeValueAsString( finalModelMap );
    }
    
    
    @RequestMapping(value = "/regcheck", method = RequestMethod.POST)
    @ResponseBody
    public String regcheck( @RequestParam (value="data", required=true) String postData,
                            @RequestParam (value="key", required=true) String key,
                            HttpServletRequest request) throws Exception
   {
        ObjectMapper mapper          = new ObjectMapper();
        Map<String, Object> modelMap = new HashMap<String, Object>();

        try {

            JSONObject json  = JsonUtil.stringToJSONObject( cipherService.SimpleAESDecode( postData, key ) );
            
			// 자동로그인 시점으로 이때는 기기ID가 있으므로 해당 기기ID를 가져와서 OS 판단
            Device dvc = getORGinfo(json.get("DeviceID").toString(), 0);

            String InstalledAppVer = json.getString("InstalledAppVer");
            int configVersoin = Integer.parseInt(android_version.replaceAll("\\.", ""));
            int installVersion = Integer.parseInt(InstalledAppVer.replaceAll("\\.", ""));

            if (dvc.getOsNameOri().equals("Android") && Integer.compare(configVersoin, installVersion) >= 0) {
            	logger.info("regcheck ERROR>> 사용 할 수 없는 앱 버전" );
            	
            	//사용 가능한 ID가 아니면
            	modelMap.put("Status", "ERROR");
            	modelMap.put("Message", MSG_STATUS_ANDROID_VERION);
            }
			
        } catch (Exception e) {
        	modelMap.put("Status", "ERROR");
    		modelMap.put("Message", MSG_STATUS_ERROR);
        }
		
        ObjectMapper finalMapper = new ObjectMapper();
        Map<String, Object> finalModelMap = new HashMap<String, Object>();
        finalModelMap.put("Data",cipherService.SimpleAESEncode(mapper.writeValueAsString(modelMap),key));
        finalModelMap.put("key",key);
        return finalMapper.writeValueAsString(finalModelMap);
    }

}