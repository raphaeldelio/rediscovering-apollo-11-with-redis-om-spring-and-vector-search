// Function to save toggle state to localStorage
function saveToggleState(targetId, isVisible) {
    localStorage.setItem(`toggle_${targetId}`, isVisible ? 'visible' : 'hidden');
}

// Function to load toggle state from localStorage
function loadToggleState(targetId) {
    return localStorage.getItem(`toggle_${targetId}`);
}

// Apply toggle states on page load
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll(".toggle-btn").forEach(button => {
        const targetId = button.getAttribute("data-target");
        const targetElements = document.querySelectorAll(`[id^="${targetId}"]`);
        const savedState = loadToggleState(targetId);

        // If there's a saved state in localStorage, use it
        if (savedState === 'visible') {
            // Apply to existing elements
            targetElements.forEach(targetElement => {
                targetElement.style.display = "block";
            });
            // Always update button state
            button.classList.add('active');
        } else if (savedState === 'hidden') {
            // Apply to existing elements
            targetElements.forEach(targetElement => {
                targetElement.style.display = "none";
            });
            // Always update button state
            button.classList.remove('active');
        } else {
            // No saved state, initialize based on current display state
            // Only check elements if they exist
            if (targetElements.length > 0) {
                // Check if any of the target elements are visible
                let isAnyVisible = false;
                targetElements.forEach(targetElement => {
                    // Get computed style to check actual display value
                    const computedStyle = window.getComputedStyle(targetElement);
                    if (computedStyle.display !== 'none') {
                        isAnyVisible = true;
                    }
                });

                // Save initial state to localStorage
                saveToggleState(targetId, isAnyVisible);

                // Set button active state based on visibility
                if (isAnyVisible) {
                    button.classList.add('active');
                } else {
                    button.classList.remove('active');
                }
            } else {
                // For buttons like "enable rag" and "enable semantic cache" that don't have elements on page load,
                // initialize them as hidden/inactive
                saveToggleState(targetId, false);
                button.classList.remove('active');
            }
        }
    });
});

document.querySelectorAll(".toggle-btn").forEach(button => {
    button.addEventListener("click", function() {
        const targetId = this.getAttribute("data-target");
        const targetElements = document.querySelectorAll(`[id^="${targetId}"]`);
        let isVisible = false;

        // If there are elements to toggle, toggle them
        if (targetElements.length > 0) {
            targetElements.forEach(targetElement => {
                isVisible = targetElement.style.display === "none";
                targetElement.style.display = isVisible ? "block" : "none";
            });
        } else {
            // If there are no elements to toggle (e.g., for rag and semantic-cache),
            // get the current state from the button's active class
            isVisible = !this.classList.contains('active');
        }

        // Toggle active class on button
        if (isVisible) {
            this.classList.add('active');
        } else {
            this.classList.remove('active');
        }

        // Save state to localStorage
        saveToggleState(targetId, isVisible);
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


// Function to check if a toggle is enabled
function isToggleEnabled(targetId) {
    const savedState = loadToggleState(targetId);
    return savedState === 'visible';
}

function sendQuery(query, imageBase64) {
    let requestBody = { query: query };
    if (imageBase64) requestBody.imageBase64 = imageBase64;

    // Create arrays to hold our fetch promises and their results
    const fetchPromises = [];

    // Only send requests for enabled search types
    if (isToggleEnabled('texts-response-column')) {
        fetchPromises.push(
            fetch("/search-by-text", {
                method: "POST",
                headers: { "Content-Type": "application/json", "HX-Request": "true" },
                body: JSON.stringify(requestBody)
            }).then(res => res.json())
        );
    } else {
        fetchPromises.push(Promise.resolve(null));
    }

    if (isToggleEnabled('questions-response-column')) {
        // Add enableRag and enableSemanticCache properties for question search
        const questionRequestBody = { ...requestBody };
        questionRequestBody.enableRag = isToggleEnabled('rag');
        questionRequestBody.enableSemanticCache = isToggleEnabled('semantic-cache');

        fetchPromises.push(
            fetch("/search-by-question", {
                method: "POST",
                headers: { "Content-Type": "application/json", "HX-Request": "true" },
                body: JSON.stringify(questionRequestBody)
            }).then(res => res.json())
        );
    } else {
        fetchPromises.push(Promise.resolve(null));
    }

    if (isToggleEnabled('summaries-response-column')) {
        // Add enableRag and enableSemanticCache properties for summary search
        const summaryRequestBody = { ...requestBody };
        summaryRequestBody.enableRag = isToggleEnabled('rag');
        summaryRequestBody.enableSemanticCache = isToggleEnabled('semantic-cache');

        fetchPromises.push(
            fetch("/search-by-summary", {
                method: "POST",
                headers: { "Content-Type": "application/json", "HX-Request": "true" },
                body: JSON.stringify(summaryRequestBody)
            }).then(res => res.json())
        );
    } else {
        fetchPromises.push(Promise.resolve(null));
    }

    if (isToggleEnabled('images-text-response-column')) {
        fetchPromises.push(
            fetch("/search-by-image-text", {
                method: "POST",
                headers: { "Content-Type": "application/json", "HX-Request": "true" },
                body: JSON.stringify(requestBody)
            }).then(res => res.json())
        );
    } else {
        fetchPromises.push(Promise.resolve(null));
    }

    if (isToggleEnabled('images-response-column')) {
        fetchPromises.push(
            fetch("/search-by-image", {
                method: "POST",
                headers: { "Content-Type": "application/json", "HX-Request": "true" },
                body: JSON.stringify(requestBody)
            }).then(res => res.json())
        );
    } else {
        fetchPromises.push(Promise.resolve(null));
    }

    Promise.all(fetchPromises).then(([textData, questionData, summaryData, imageTextData, imageData]) => {
        console.log("Text Response:", textData);
        console.log("Question Response:", questionData);
        console.log("Summary Response:", summaryData);
        console.log("Image Response:", imageData);
        console.log("Image Text Response:", imageTextData);

        // Render text-based response if enabled
        if (textData) {
            let textsHtml = `
                <p><strong>Processing time:</strong> ${textData.processingTime}</p>
                <p><strong>Q:</strong> ${textData.query}</p>`;

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
        }

        // Render question-based response if enabled
        if (questionData) {
            let questionsHTML = `<p><strong>Processing time:</strong> ${questionData.processingTime}</p>
                <p><strong>Q:</strong> ${questionData.query}</p>`;

            if (isToggleEnabled('rag')) {
                questionsHTML += `<p><strong>A:</strong> ${questionData.ragAnswer}</p>`;
            }

            if (isToggleEnabled('semantic-cache')) {
                questionsHTML += `
                    <div>
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
        }

        // Render summary-based response if enabled
        if (summaryData) {
            let summariesHTML = `
                    <p><strong>Processing time:</strong> ${summaryData.processingTime}</p>
                    <p><strong>Q:</strong> ${summaryData.query}</p>`;

            if (isToggleEnabled('rag')) {
                summariesHTML += `
                    <p><strong>A:</strong> ${summaryData.ragAnswer}</p>`;
            }

            if (isToggleEnabled('semantic-cache')) {
                summariesHTML += `
                    <div>
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
        }

        // Render image-based response if enabled
        if (imageData) {
            let imagesHTML = `<p><strong>Processing time:</strong> ${imageData.processingTime}</p>
                    <h4>Matched Images:</h4><ul>`;

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
        }

        // Render image-text-based response if enabled
        if (imageTextData) {
            let imagesTextHTML = `<p><strong>Processing time:</strong> ${imageTextData.processingTime}</p>
                <p><strong>Q:</strong> ${imageTextData.query}</p><h4>Matched Images:</h4><ul>`;

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
        }

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
