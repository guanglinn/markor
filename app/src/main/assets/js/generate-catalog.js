function getCatalogHtml() {
    let headers = document.querySelectorAll('h1, h2, h3, h4, h5, h6');
    if(headers.length == 0) {
    	return "";
    }

    let padding = [0, 16, 32, 48, 64, 80, 96];
    let level = 0;
    let id = 0;
    let catalog = document.createElement("div");
    for (let i = 0; i < headers.length; i++) {
        level = parseInt(headers[i].nodeName.charAt(1));
        let h = document.createElement("h" + level);
        h.setAttribute('style', 'padding-left: ' + padding[level - 1] + "px");

        let a = document.createElement("a");
        a.innerText = headers[i].innerText;
        a.setAttribute("href", "#a" + id);
        a.setAttribute("onclick", "notify(this.getAttribute('href'))");
        headers[i].setAttribute("id", "a" + id);
        id++;

        h.appendChild(a);
        catalog.appendChild(h);
    }
    let temp = "<html><head></head><body>";
    temp += catalog.innerHTML;
    temp += "<script>function notify(data){injectedObject.run(data);}</script>";
    temp += "</body></html>";
    return temp;
}
