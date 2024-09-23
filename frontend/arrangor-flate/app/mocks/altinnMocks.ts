import { http, HttpResponse, PathParams } from "msw";
import { Rolletilgang, RolleTilgangRequest, RolleType } from "../domene/domene";

export const mulighetsrommetAltinnAclHandlers = [
  http.post<PathParams, RolleTilgangRequest, Rolletilgang>(
    "*/mulighetsrommet-altinn-acl/api/v1/rolle/tiltaksarrangor",
    () =>
      HttpResponse.json({
        roller: [
          { organisasjonsnummer: "123456789", roller: [RolleType.TILTAK_ARRANGOR_REFUSJON] },
        ],
      }),
  ),
];
