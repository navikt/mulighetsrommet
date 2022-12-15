import { Topic } from "../domain";
import * as console from "console";

export const getTopics = async () =>
  fetch("/mulighetsrommet-arena-adapter/topics", {
    method: "GET",
  }).then((response) => response.json());

export const putTopicRunningState = async (topics: Topic[]) => {
  return fetch("/mulighetsrommet-arena-adapter/topics", {
    method: "PUT",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify(topics),
  });
};

export const replayEvents = async () => {
  return fetch("http://0.0.0.0:8084/api/topics/replay", {
    method: "GET",
  }).then((response) => console.log(response.json()));
};
