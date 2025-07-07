class MOInputViewController: UIViewController {

    let arrProtocols:[String] = ["https://", "http://"];
    let nameProtocols:[String] = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"];
    let urlProtocols:[String] = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"];
	
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
		self.urlAddrText.text = self.arrProtocols[0] + self.urlProtocols[0] + "/URL"
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
        self.urlAddrText.text = self.arrProtocols[0] + self.urlProtocols[row] + "/URL"
    }
	
}
