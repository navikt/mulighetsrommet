import { Topic } from "../domain";

export const getTopics = async () =>
  fetch("/mulighetsrommet-arena-adapter/topics", {
    method: "GET",
  }).then((response) => response.json());

export const getArenaTables = async () =>
  fetch("/mulighetsrommet-arena-adapter/arena-tables", {
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

export const replayEvents = async (
  arenaTable: string | null,
  status: string | null,
) => {
  return await fetch("/mulighetsrommet-arena-adapter/events/replay", {
    method: "PUT",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify({
      table: arenaTable,
      status,
    }),
  });
};

export const replayEvent = async (arenaTable: string, arenaId: string) => {
  return await fetch(`/mulighetsrommet-arena-adapter/event/replay`, {
    method: "PUT",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify({
      table: arenaTable,
      arenaId: arenaId,
    }),
  });
};

export const deleteEvents = async (arenaTable: string, arenaIds: string) => {
  const arenaIdsAsList = arenaIds.split(",").map((id) => id.trim());
  return await fetch(`/mulighetsrommet-arena-adapter/events`, {
    method: "DELETE",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify({
      table: arenaTable,
      arenaIds: arenaIdsAsList,
    }),
  });
};

export const syncVirksomhet = async (orgnr: string) => {
  return await fetch(
    `/mulighetsrommet-api/api/v1/internal/virksomhet/update?orgnr=${orgnr}`,
    {
      method: "GET",
      headers: {
        "content-type": "application/json",
      },
    },
  );
};
