import { DefaultBodyType, PathParams, rest } from "msw";
import { Virksomhet, VirksomhetKontaktperson } from "mulighetsrommet-api-client";
import { mockVirksomheter } from "../fixtures/mock_virksomheter";
import { mockVirksomhetKontaktperson } from "../fixtures/mock_virksomhet_kontaktperson";

export const virksomhetHandlers = [
  rest.get<DefaultBodyType, { sok: string }, Virksomhet[]>(
    "*/api/v1/internal/virksomhet/sok/:sok",
    (req, res, ctx) => {
      return res(
        ctx.status(200),
        ctx.json(
          Object.values(mockVirksomheter).filter(
            (enhet) => enhet.navn?.toLowerCase().includes(req.params.sok.toLocaleLowerCase()),
          ),
        ),
      );
    },
  ),
  rest.get<DefaultBodyType, PathParams, Virksomhet | undefined>(
    "*/api/v1/internal/virksomhet/:orgnr",
    (req, res, ctx) => {
      return res(
        ctx.status(200),
        ctx.json(
          Object.values(mockVirksomheter).find(
            (enhet) => enhet.organisasjonsnummer === req.params.orgnr,
          ),
        ),
      );
    },
  ),
  rest.get<DefaultBodyType, PathParams, Virksomhet[] | undefined>(
    "*/api/v1/internal/virksomhet",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(Object.values(mockVirksomheter)));
    },
  ),

  rest.get<DefaultBodyType, PathParams, VirksomhetKontaktperson[]>(
    "*/api/v1/internal/*/kontaktperson",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockVirksomhetKontaktperson));
    },
  ),
];
