import { Topic } from "../domain";
import toast from "react-hot-toast";
import { ErrorToast } from "../components/Toast";
import { v4 as uuidv4 } from "uuid";

export class ApiError extends Error {
  constructor(
    public status: number,
    public statusText: string,
    message: string,
  ) {
    super(message);
  }
}

export const getTopics = () =>
  fetch("/mulighetsrommet-arena-adapter/topics", {
    method: "GET",
    headers: getDefaultHeaders(),
  })
    .then(parseJson)
    .catch((error) => toastError("Klarte ikke laste Topics", error));

export const getArenaTables = () =>
  fetch("/mulighetsrommet-arena-adapter/arena-tables", {
    method: "GET",
    headers: getDefaultHeaders(),
  })
    .then(parseJson)
    .catch((error) => toastError("Klarte ikke laste ArenaTables", error));

export const putTopicRunningState = (topics: Topic[]) =>
  fetch("/mulighetsrommet-arena-adapter/topics", {
    method: "PUT",
    headers: {
      ...getDefaultHeaders(),
      "content-type": "application/json",
    },
    body: JSON.stringify(topics),
  })
    .then(checkOk)
    .then(() => toast.success("Topics oppdatert"))
    .catch((error) => toastError("Klarte ikke oppdatere topics", error));

export const replayEvents = (arenaTable: string | null, status: string | null) =>
  fetch("/mulighetsrommet-arena-adapter/events/replay", {
    method: "PUT",
    headers: {
      ...getDefaultHeaders(),
      "content-type": "application/json",
    },
    body: JSON.stringify({
      table: arenaTable,
      status,
    }),
  })
    .then(checkOk)
    .then(() => toast.success("Gjenspilling startet"))
    .catch((error) => toastError("Klarte ikke starte gjenspilling av events", error));

export const replayEvent = (arenaTable: string, arenaId: string) =>
  fetch("/mulighetsrommet-arena-adapter/event/replay", {
    method: "PUT",
    headers: {
      ...getDefaultHeaders(),
      "content-type": "application/json",
    },
    body: JSON.stringify({
      table: arenaTable,
      arenaId: arenaId,
    }),
  })
    .then(checkOk)
    .then(() => toast.success("Event gjenspilt"))
    .catch((error) => {
      toastError(
        `Klarte ikke starte gjenspille event med table=${arenaTable} id=${arenaId}`,
        error,
      );
    });

export const deleteEvents = async (arenaTable: string, arenaIds: string) => {
  const arenaIdsAsList = arenaIds
    .split(/[,\s]/)
    .map((id) => id.trim())
    .filter((id) => id.length);

  return fetch(`/mulighetsrommet-arena-adapter/events`, {
    method: "DELETE",
    headers: {
      ...getDefaultHeaders(),
      "content-type": "application/json",
    },
    body: JSON.stringify({
      table: arenaTable,
      arenaIds: arenaIdsAsList,
    }),
  })
    .then(checkOk)
    .then(() => toast.success("Events slettet"))
    .catch((error) => toastError("Klarte ikke slette events", error));
};

export type MrApiTask =
  | "generate-validation-report"
  | "initial-load-tiltaksgjennomforinger"
  | "initial-load-mulighetsrommet-tiltaksgjennomforinger"
  | "sync-navansatte";

export const runTask = (task: MrApiTask) =>
  fetch(`/mulighetsrommet-api/api/v1/internal/tasks/${task}`, {
    method: "POST",
    headers: getDefaultHeaders(),
  })
    .then(parseJson)
    .then((response) => {
      toast.success(`Scheduled task '${task}': ${response.id}`);
    })
    .catch((error) => {
      toastError(`Failed to execute task '${task}'`, error);
    });

export const syncVirksomhet = (orgnr: string) =>
  fetch(`/mulighetsrommet-api/api/v1/internal/virksomhet/update?orgnr=${orgnr}`, {
    method: "POST",
    headers: getDefaultHeaders(),
  })
    .then(checkOk)
    .then(() => toast.success("Virksomhet synkronisert"))
    .catch((error) => {
      toastError(`Klarte ikke synkronisere virksomhet med orgnr=${orgnr}`, error);
    });

function getDefaultHeaders(): Record<string, string> {
  return {
    Accept: "application/json",
    "Nav-Consumer-Id": "MAAM",
    "Nav-Call-Id": uuidv4(),
  };
}

function toastError(message: string, error: ApiError | Error) {
  toast.error(() => <ErrorToast title={message} error={error} />);
}

async function checkOk(response: Response) {
  if (!response.ok) {
    throw new ApiError(response.status, response.statusText, await response.text());
  }
}

async function parseJson(response: Response) {
  if (!response.ok) {
    throw new ApiError(response.status, response.statusText, await response.text());
  }

  return response.json();
}
