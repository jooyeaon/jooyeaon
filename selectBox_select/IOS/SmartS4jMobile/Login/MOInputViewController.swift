class MOInputViewController: UIViewController {

    let arrProtocols:[String] = ["https://", "http://"];
    let nameProtocols:[String] = ["조선내화", "대한소결금속", "삼한", "화인태크", "시알이노테크", "선화이엔지", "선우이엔지", "화인로", "IPCR", "CCR"];
    let urlProtocols:[String] = ["mcrworks.crholdings.co.kr", "mksmworks.crholdings.co.kr", "mshworks.crholdings.co.kr", "mftcworks.crholdings.co.kr", "mcrinnoworks.crholdings.co.kr", "msunhwaworks.crholdings.co.kr", "msunwooworks.crholdings.co.kr", "mfineroworks.crholdings.co.kr", "mipcrworks.crholdings.co.kr", "mccrworks.crholdings.co.kr"];
    
	
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
		self.urlAddrText.text = self.arrProtocols[0] + self.urlProtocols[0] + "/mobileapp"
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        if let gwPushUrl = UserDefaults.standard.string(forKey: "push_server"), gwPushUrl != "" {
            urlAddrText.text = gwPushUrl.components(separatedBy: "://")[1]
        } else {
            self.urlProtocolText.text = self.nameProtocols[0]
        }
    }

    //MARK: - Protocol Picker
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        return 1
    }
    
    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        return nameProtocols.count
    }
    
    func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
        dismissKeyboard()
        return nameProtocols[row]
    }
    
    func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
        self.urlAddrText.text = self.arrProtocols[0] + self.urlProtocols[row] + "/mobileapp"
    }
	
}
