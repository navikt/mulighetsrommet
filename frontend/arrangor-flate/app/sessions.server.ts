import { createCookieSessionStorage } from "react-router";

interface DrifttilskuddSessionData {
  orgnr?: string;
  gjennomforingId?: string;
  tilsagnId?: string;
  periodeStart?: string;
  periodeSlutt?: string;
  periodeInklusiv?: string;
  vedlegg?: File[];
  belop?: string;
  valuta?: string;
  kontonummer?: string;
  kid?: string;
  bekreftelse?: boolean;
}

type SessionFlashData = {
  error: string;
};

const { getSession, commitSession, destroySession } = createCookieSessionStorage<
  DrifttilskuddSessionData,
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
