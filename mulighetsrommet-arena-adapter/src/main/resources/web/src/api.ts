export const getTopics = async () =>
  fetch("http://localhost:8084/manager/topics", {
    method: "GET",
  }).then((response) => response.json());
