@echo off
echo --- Compiling Zero-Copy Project ---

:: 1. สร้างไดเรกทอรี 'bin' (ถ้ายังไม่มี) เพื่อเก็บไฟล์ .class
if not exist bin (
    mkdir bin
)

:: 2. คอมไพล์โปรเจกต์
:: -d bin : สั่งให้ javac เก็บไฟล์ .class ที่คอมไพล์แล้วไว้ในโฟลเดอร์ 'bin'
:: zerocopy/Main.java : ระบุไฟล์หลัก (javac จะฉลาดพอที่จะหาและคอมไพล์ไฟล์อื่นๆ ที่จำเป็นทั้งหมด เช่น Client, Server, Protocol)
javac -d bin zerocopy/Main.java

:: 3. ตรวจสอบว่าคอมไพล์สำเร็จหรือไม่
if %errorlevel% neq 0 (
    echo.
    echo !!! --- COMPILATION FAILED --- !!!
    goto :eof
)

echo --- Compilation Successful ---
echo.
echo --- Running Java Application (zerocopy.Main) ---
echo.

:: 4. รันโปรแกรม
:: -cp bin : บอก Java ให้หา .class ไฟล์ในโฟลเดอร์ 'bin' (cp = Class Path)
:: zerocopy.Main : ชื่อคลาสหลักที่ต้องการรัน
:: %* : ส่งผ่าน arguments ทั้งหมดที่ .bat ได้รับ (เช่น -s, -p 9090) ไปให้โปรแกรม Java
java -cp bin zerocopy.Main %*