$smtpClient = New-Object System.Net.Mail.SmtpClient('smtp.gmail.com', 587)
$smtpClient.EnableSsl = $true
$smtpClient.Credentials = New-Object System.Net.NetworkCredential('wasimakaram360@gmail.com', 'ulqikhicfpguzgfj')
$msg = New-Object System.Net.Mail.MailMessage
$msg.From = 'wasimakaram360@gmail.com'
$msg.To.Add('wasimakaram360@gmail.com')
$msg.Subject = 'CodeCollab SMTP Test'
$msg.Body = 'If you see this, SMTP is working!'
try {
    $smtpClient.Send($msg)
    Write-Host 'SUCCESS: Email sent!'
} catch {
    Write-Host 'FAILED:' $_.Exception.Message
    Write-Host 'Inner:' $_.Exception.InnerException.Message
}
