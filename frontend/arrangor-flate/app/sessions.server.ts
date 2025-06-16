import { createCookieSessionStorage } from "react-router";

export type InnsendingSessionData = {
  orgnr?: string;
  gjennomforingId?: string;
  tilsagnId?: string;
  periodeStart?: string;
  periodeSlutt?: string;
  vedlegg?: File[];
  belop?: string;
  kontonummer?: string;
  kid?: string;
  bekreftelse?: boolean;
};

type SessionFlashData = {
  error: string;
};

const { getSession, commitSession, destroySession } = createCookieSessionStorage<
  InnsendingSessionData,
  SessionFlashData
>({
  cookie: {
    name: "innsending-session",
    httpOnly: true,
    sameSite: "strict",
    secure: true,
  },
});

export { getSession, commitSession, destroySession };
