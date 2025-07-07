class UserLoginViewController: UIViewController {

    //MARK: - ViewController Update
    private func initView() {
        // 고객사별 로그인 이미지 별로도 표현
        let loginImageURL = "https://고객사URL/imageURL"
        
        let urlPosition = "selectbox 선택칸"
        
        let loginImageURL2 = loginImageURL + "login_img_" + urlPosition + ".jpg";
        
        loginImage.downloaded(from: URL(string: loginImageURL2)!)
    }
}
