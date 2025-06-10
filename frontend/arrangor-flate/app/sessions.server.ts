import { createCookieSessionStorage } from "react-router";

type InnsendingSessionData = {
  orgnr?: string;
  gjennomforingId?: string;
  tilsagnId?: string;
  periodeStart?: string;
  periodeSlutt?: string;
  vedlegg?: File[];
  beskrivelse?: string;
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
  // a Cookie from `createCookie` or the CookieOptions to create one
  cookie: {
    name: "__session",

    // all of these are optional
    //domain: "reactrouter.com",
    // Expires can also be set (although maxAge overrides it when used in combination).
    // Note that this method is NOT recommended as `new Date` creates only one date on each server deployment, not a dynamic date in the future!
    //
    // expires: new Date(Date.now() + 60_000),
    httpOnly: true,
    //maxAge: 60,
    //path: "/",
    //sameSite: "lax",
    //secrets: ["s3cret1"],
    secure: true,
  },
});

export { getSession, commitSession, destroySession };
