// Establish connection with app

const res720Btn = document.getElementById("res720p");

const myFace = document.getElementById("myFace");
const canvas = document.getElementById('canvas');

function capturePic() {
    const context = canvas.getContext('2d');
    context.drawImage(myFace, 0, 0, 1920, 1080);
    return context.getImageData(0, 0, 1920, 1080).data;
}

let port = browser.runtime.connectNative("browser");
port.onMessage.addListener(response => {
    // Let's just echo the message back

    var data;
    if(response.resolution === '720'){
        res720Btn.click();
        data = capturePic();
    }
    port.postMessage(`Received: ${data}`);
});

const testBtn = document.getElementById('test');
testBtn.addEventListener("click", function(){
    port.postMessage("Hello from WebExtension! " + testBtn);
});

