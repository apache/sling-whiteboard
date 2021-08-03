/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
TLN.append_line_numbers("source");

const sourceEl = document.getElementById("source");
const resultsContainer = document.getElementById("results-container");
const parsedEl = document.getElementById("parsed");
const featureEl = document.getElementById("feature");
const featureContainerEl = document.getElementById("feature-container");
const goButton = document.getElementById("evaluate-repoinit");
const executeCbx = document.getElementById("execute");
const messagesEl = document.getElementById("messages");

goButton.addEventListener("click", async function () {
  goButton.disabled = true;
  sourceEl.disabled = true;
  resultsContainer.classList.add("d-none");
  featureContainerEl.classList.add("d-none");
  const res = await fetch(`repoinit?execute=${executeCbx.checked}`, {
    method: "post",
    body: sourceEl.value,
  });
  const json = await res.json();

  messagesEl.innerHTML = "";
  json.messages.forEach((m) => {
    const par = document.createElement("p");
    par.innerText = m;
    messagesEl.appendChild(par);
  });

  if (json.succeeded) {
    parsedEl.innerText = JSON.stringify(json.operations, null, 2);
    featureEl.innerText = JSON.stringify(
      {
        "repoinit:TEXT|true": sourceEl.value.split("\n"),
      },
      null,
      2
    );
    featureContainerEl.classList.remove("d-none");
  } else {
    parsedEl.innerText = "";
    featureEl.innerText = "";
  }
  resultsContainer.classList.remove("d-none");
  goButton.disabled = false;
  sourceEl.disabled = false;
});
