import smtplib

receiver = 'chinmay.todankar98@gmail.com'
password = 'darkenergy'
sender = 'elrosario.rosario@gmail.com'

message = """ From: From Elnino <elrosario.rosario@gmail.com>
To: To Chinmay <chinmay.todankar98@gmail.com>
Subject: SMTP e-mail test

This is a test e-mail message."""

try:
    smtpObj = smtplib.SMTP('smtp.gmail.com', 587)
    smtpObj.ehlo()
    smtpObj.starttls()
    smtpObj.login(sender,password)
    smtpObj.sendmail(sender,receiver,message)
    print("Successfully sent email")
    smtpObj.quit()
except Exception:
    print("Error: unable to send email")
