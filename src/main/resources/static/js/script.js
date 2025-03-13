document.querySelectorAll(".toggle-btn").forEach(button => {
    button.addEventListener("click", function() {
        const targetId = this.getAttribute("data-target");
        const targetElements = document.querySelectorAll(`[id^="${targetId}"]`);

        targetElements.forEach(targetElement => {
            targetElement.style.display = (targetElement.style.display === "none") ? "block" : "none";
        });
    });
});

document.getElementById("query-form").addEventListener("submit", function(event) {
    event.preventDefault();

    let queryText = document.getElementById("query-input").value;
    let imageFile = document.getElementById("image-input").files[0];

    if (imageFile) {
        let reader = new FileReader();
        reader.readAsDataURL(imageFile);
        reader.onload = function() {
            let imageBase64 = reader.result.split(",")[1];
            sendQuery(queryText, imageBase64);
        };
        reader.onerror = function(error) {
            console.error("Error converting image:", error);
        };
    } else {
        sendQuery(queryText, null);
    }
});


function sendQuery(query, imageBase64) {
    let requestBody = { query: query };
    if (imageBase64) requestBody.imageBase64 = imageBase64;

    Promise.all([
        fetch("/search-by-text", {
            method: "POST",
            headers: { "Content-Type": "application/json", "HX-Request": "true" },
            body: JSON.stringify(requestBody)
        }).then(res => res.json()),

        fetch("/search-by-question", {
            method: "POST",
            headers: { "Content-Type": "application/json", "HX-Request": "true" },
            body: JSON.stringify(requestBody)
        }).then(res => res.json()),

        fetch("/search-by-summary", {
            method: "POST",
            headers: { "Content-Type": "application/json", "HX-Request": "true" },
            body: JSON.stringify(requestBody)
        }).then(res => res.json()),

        fetch("/search-by-image-text", {
            method: "POST",
            headers: { "Content-Type": "application/json", "HX-Request": "true" },
            body: JSON.stringify(requestBody)
        }).then(res => res.json()),

        fetch("/search-by-image", {
            method: "POST",
            headers: { "Content-Type": "application/json", "HX-Request": "true" },
            body: JSON.stringify(requestBody)
        }).then(res => res.json())
    ]).then(([textData, questionData, summaryData, imageTextData, imageData]) => {
        console.log("Text Response:", textData);
        console.log("Question Response:", questionData);
        console.log("Summary Response:", summaryData);
        console.log("Image Response:", imageData);
        console.log("Image Text Response:", imageTextData);

        // Render text-based response
        let textsHtml = `
                <p><strong>Q:</strong> ${textData.query}</p>`

        if (textData.matchedTexts && textData.matchedTexts.length > 0) {
            textsHtml += `<h4>Matched Texts:</h4><ul>`;
            textsHtml += textData.matchedTexts.map(q => `
                    <li>
                       ${q.text} (Score: ${parseFloat(q.score).toFixed(2)})
                    </li>
                    `).join("");
            textsHtml += `</ul>`;
        }

        document.getElementById("texts-response").innerHTML = textsHtml;

        // Render question-based response
        let questionsHTML = `
                <p><strong>Q:</strong> ${questionData.query}</p>
                <p id="rag-question" style="display: none"><strong>A:</strong> ${questionData.answer}</p>`;

        if (questionData.cachedQuery && questionData.cachedQuery.length > 0) {
            questionsHTML += `
                <div id="semantic-cache-question" style="display: none">
                <p><strong>Cached Query: </strong>${questionData.cachedQuery}</p>
                <p><strong>Cached Score: </strong>Cached Score: ${questionData.cachedScore}</p>
                </div>`;
        }

        if (questionData.matchedQuestions && questionData.matchedQuestions.length > 0) {
            questionsHTML += `<h4>Matched Questions:</h4><ul>`;
            questionsHTML += questionData.matchedQuestions.map(q => `
                    <li>
                        <button class="collapsible">${q.question} (Score: ${parseFloat(q.score).toFixed(2)})</button>
                        <div class="content">
                            <p><strong>Related Utterances:</strong></p>
                            <ul>
                                ${q.utterances.split("\n").map(utterance => `<li>${utterance}</li>`).join("")}
                            </ul>
                        </div>
                    </li>
                    `).join("");
            questionsHTML += `</ul>`;
        }

        document.getElementById("questions-response").innerHTML = questionsHTML;

        // Render summary-based response
        let summariesHTML = `
                <p><strong>Q:</strong> ${summaryData.query}</p>
                <p id="rag-question" style="display: none"><strong>A:</strong> ${summaryData.answer}</p>`;

        if (summaryData.cachedQuery && summaryData.cachedQuery.length > 0) {
            summariesHTML += `
                <div id="semantic-cache-summary" style="display: none">
                <p><strong>Cached Query: </strong>${summaryData.cachedQuery}</p>
                <p><strong>Cached Score: </strong>Cached Score: ${summaryData.cachedScore}</p>
                </div>`;
        }

        if (summaryData.matchedSummaries && summaryData.matchedSummaries.length > 0) {
            summariesHTML += `<h4>Matched Summaries:</h4><ul>`;
            summariesHTML += summaryData.matchedSummaries.map(s => `
                    <li>
                        <button class="collapsible">${s.summary} (Score: ${parseFloat(s.score).toFixed(2)})</button>
                        <div class="content">
                            <p><strong>Related Utterances:</strong></p>
                            <ul>
                                ${s.utterances.split("\n").map(utterance => `<li>${utterance}</li>`).join("")}
                            </ul>
                        </div>
                    </li>
                    `).join("");
            summariesHTML += `</ul>`;
        }

        document.getElementById("summaries-response").innerHTML = summariesHTML;


        // Render image-based response
        let imagesHTML = `<h4>Matched Images:</h4><ul>`;

        if (imageData.matchedPhotographs && imageData.matchedPhotographs.length > 0) {
            imagesHTML += imageData.matchedPhotographs.map(img => `
                    <li>
                        <p><strong>Image Path:</strong> ${img.imagePath} (Score: ${parseFloat(img.score).toFixed(2)})</p>
                        <img src="${img.imagePath}" alt="Matched Image" style="width:100%;max-width:300px;border-radius:8px;">
                        <p><strong>Description:</strong> ${img.description}</p>
                    </li>
                `).join("");
        } else {
            imagesHTML += `<p><em>No matching images found.</em></p>`;
        }

        imagesHTML += `</ul>`;

        document.getElementById("images-response").innerHTML = imagesHTML;

        // Render image-text-based response
        let imagesTextHTML = `<p><strong>Q:</strong> ${imageTextData.query}</p><h4>Matched Images:</h4><ul>`;
        if (imageTextData.matchedPhotographs && imageTextData.matchedPhotographs.length > 0) {
            imagesTextHTML += imageTextData.matchedPhotographs.map(img => `
                    <li>
                        <p><strong>Image Path:</strong> ${img.imagePath} (Score: ${parseFloat(img.score).toFixed(2)})</p>
                        <img src="${img.imagePath}" alt="Matched Image" style="width:100%;max-width:300px;border-radius:8px;">
                        <p><strong>Description:</strong> ${img.description}</p>
                    </li>
                `).join("");
        } else {
            imagesTextHTML += `<p><em>No matching images found.</em></p>`;
        }
        imagesTextHTML += `</ul>`;
        document.getElementById("images-text-response").innerHTML = imagesTextHTML;

        attachCollapsibleListeners();
    })
        .catch(error => console.error("Error:", error));
}

// ✅ Function to attach event listeners to collapsible buttons
function attachCollapsibleListeners() {
    document.querySelectorAll(".collapsible").forEach(button => {
        button.addEventListener("click", function() {
            this.nextElementSibling.classList.toggle("active");
        });
    });
}

// ✅ Call this function initially in case collapsibles exist on page load
attachCollapsibleListeners();