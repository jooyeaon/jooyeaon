class GwNoticeViewController: UIViewController {

    let secureScreen = SecureScreen()
    
    //MARK: - ViewController Cycle
    
	override func viewWillDisappear(_ animated: Bool) {
		// 캡쳐 방지 종료
    }
    
    override func viewWillAppear(_ animated: Bool) {
        // 캡쳐 방지 리스너 시작
		// 캡쳐 방지 캡쳐 방지 시작
    }

    
    override func viewDidLoad() {
        super.viewDidLoad()
            
       getSecureScreenLicense()
    }
    
    //MARK: - 캡쳐방지
    func getSecureScreenLicense() {
        let licenseResult = "라이센스 파일 읽기"
        
        var message = "";
		// licenseResult 체크
		
        // 라이센스 값이 정상이 아니면 앱 종료
        if (licenseResult ?? 0 < 0) {
            DispatchQueue.main.async (execute: {
                let errAC = UIAlertController(title: "app_name".localized, message: message, preferredStyle:UIAlertControllerStyle.alert )
                let errAction = UIAlertAction(title: "common_alert_ok".localized, style: .default, handler: {action in
                    exit(EXIT_SUCCESS)
                })
                errAC.addAction(errAction)
                self.present(errAC, animated: true, completion: nil)
            })
        }
    }
    
}
