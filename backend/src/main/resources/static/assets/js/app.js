function checkLogin() {
  const token = localStorage.getItem('token')

  if (!token) {
    window.location.href = '/auth/login.html'
  }
}

window.app = {
  checkLogin
}