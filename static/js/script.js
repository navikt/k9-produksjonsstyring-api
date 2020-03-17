function toggle(kode) {
    let elementId = "Checkbox" + kode;
    const xhr = new XMLHttpRequest();
    xhr.open("POST", '/mock/toggleaksjonspunkt', true);
    xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xhr.onreadystatechange = function () { // Call a function when the state changes.
        if (this.readyState === XMLHttpRequest.DONE && this.status === 200) {
            // Request finished. Do processing here.
        }
    }
    xhr.send(JSON.stringify(
        {
            "kode": kode,
            "toggle": document.getElementById(elementId).checked,
            "eksternid": document.getElementById("uuid").value,
            "aktørid": document.getElementById("aktørid").value
        }));
}