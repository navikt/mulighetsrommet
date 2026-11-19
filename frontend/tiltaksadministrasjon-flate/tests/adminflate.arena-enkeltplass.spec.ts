import test, { expect, Page } from "@playwright/test";
import {
  ArenaEnkeltplassDto,
  DataElementStatusVariant,
  DeltakerStatusType,
  GjennomforingHandling,
  GjennomforingType,
  Gradering,
  PrismodellType,
  Rolle,
  Tiltakskode,
} from "@tiltaksadministrasjon/api-client";
import { mockRedaktor } from "@/mocks/fixtures/mock_ansatt";
import { paginertMockGjennomforinger } from "@/mocks/fixtures/mock_gjennomforinger";

const kandidat: ArenaEnkeltplassDto = {
  id: "a7d63fb0-4366-412c-84b7-7c15518ee361",
  tiltaksnummer: "2026#42",
  navn: "Opplæring fra Arena",
  tiltakstype: {
    id: "76c46f41-5b58-4076-abfe-5299bc20cb97",
    tiltakskode: Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
    navn: "Enkeltplass AMO",
  },
  arrangor: {
    id: "d9d4db51-3564-4493-b897-4fc38dc48965",
    organisasjonsnummer: "992943084",
    navn: "Testarrangør",
    slettet: false,
  },
  ansvarligEnhet: "0400",
  deltakerStatus: DeltakerStatusType.DELTAR,
  startDato: "2026-01-01",
  sluttDato: null,
  deltaker: {
    id: "9f1fb081-64b3-4521-a728-f0bd018849ba",
    navn: "Test Testesen",
    norskIdent: "12345678901",
    oppfolgingEnhet: null,
    status: { value: "Deltar", variant: DataElementStatusVariant.INFO, description: null },
    innholdAnnet: null,
    avvistGrunn: null,
    gradering: Gradering.UGRADERT,
    startDato: "2026-01-01",
    sluttDato: null,
    navVeilederNavn: null,
    dagerPerUke: null,
  },
};

async function mockSok(
  page: Page,
  {
    kandidater = [kandidat],
    eksisterendeTreff = false,
    kanMigrere = true,
    harSaksbehandlerrolle = false,
    oppslagsfeil = false,
    migreringsfeil = false,
    avventOppslag,
  }: {
    kandidater?: ArenaEnkeltplassDto[];
    eksisterendeTreff?: boolean;
    kanMigrere?: boolean;
    harSaksbehandlerrolle?: boolean;
    oppslagsfeil?: boolean;
    migreringsfeil?: boolean;
    avventOppslag?: Promise<void>;
  } = {},
) {
  const oppslag: string[] = [];
  const migreringer: unknown[] = [];
  await page.exposeFunction("registrerArenaOppslag", async (search: string) => {
    oppslag.push(search);
    await avventOppslag;
  });
  await page.exposeFunction("registrerMigrering", (request: unknown) => migreringer.push(request));
  await page.addInitScript(
    ({
      kandidater,
      eksisterendeTreff,
      eksisterendeEnkeltplass,
      ansatt,
      handlinger,
      oppslagsfeil,
      migreringsfeil,
    }) => {
      const originalFetch = window.fetch.bind(window);
      const callbacks = window as unknown as {
        registrerArenaOppslag: (search: string) => Promise<void>;
        registrerMigrering: (request: unknown) => Promise<void>;
      };
      window.fetch = async (input, init) => {
        const request = new Request(input, init);
        const path = new URL(request.url).pathname;
        if (path === "/api/tiltaksadministrasjon/ansatt/me") {
          return Response.json(ansatt);
        }
        if (path === "/api/tiltaksadministrasjon/enkeltplasser/handlinger") {
          return Response.json(handlinger);
        }
        if (path === "/api/tiltaksadministrasjon/gjennomforinger" && request.method === "POST") {
          return Response.json({
            data: eksisterendeTreff ? [eksisterendeEnkeltplass] : [],
            pagination: {
              totalCount: eksisterendeTreff ? 1 : 0,
              totalPages: eksisterendeTreff ? 1 : 0,
              page: 1,
              pageSize: 10,
            },
          });
        }
        if (path === "/api/tiltaksadministrasjon/enkeltplasser/migrering/sok") {
          const { search } = await request.json();
          await callbacks.registrerArenaOppslag(search);
          if (oppslagsfeil) {
            return Response.json(
              { status: 502, detail: "Kunne ikke hente deltakelser fra Komet" },
              { status: 502 },
            );
          }
          return Response.json(kandidater);
        }
        if (/\/enkeltplasser\/[^/]+\/migrer$/.test(path)) {
          await callbacks.registrerMigrering(await request.json());
          if (migreringsfeil) {
            return Response.json(
              {
                type: "validation-error",
                status: 400,
                errors: [{ pointer: "/totalbelop", detail: "Kontroller totalbeløpet" }],
              },
              { status: 400 },
            );
          }
          return Response.json({ id: path.split("/").at(-2) });
        }
        return originalFetch(request);
      };
    },
    {
      kandidater,
      eksisterendeTreff,
      eksisterendeEnkeltplass: {
        ...paginertMockGjennomforinger.data[0],
        id: "5ece8e9b-0f76-4eba-9642-9cbe91ccb87a",
        navn: "Eksisterende enkeltplass",
        type: GjennomforingType.ENKELTPLASS,
        tiltakstype: kandidat.tiltakstype,
      },
      oppslagsfeil,
      migreringsfeil,
      handlinger: kanMigrere ? [GjennomforingHandling.MIGRER_ENKELTPLASS] : [],
      ansatt: {
        ...mockRedaktor,
        roller: [
          ...mockRedaktor.roller.filter(
            ({ rolle }) => rolle !== Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS,
          ),
          ...(harSaksbehandlerrolle
            ? [
                {
                  rolle: Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS,
                  navn: "Saksbehandler enkeltplass",
                },
              ]
            : []),
        ],
      },
    },
  );
  await page.setViewportSize({ width: 1920, height: 1080 });
  await page.goto("/gjennomforinger");
  return { oppslag, migreringer };
}

test("identitetssøk uten treff tilbyr alle Arena-matcher og skjuler dem når søket endres", async ({
  page,
}) => {
  const { oppslag } = await mockSok(page, {
    kandidater: [
      kandidat,
      { ...kandidat, id: "94e1dc18-9791-4412-bc54-7151489a4956", tiltaksnummer: "2026#43" },
    ],
  });
  const search = page.getByRole("searchbox", { name: "Søk etter tiltaksgjennomføring" });
  await search.fill("12345678901");
  await expect(page.getByRole("heading", { name: "Fant 2 gjennomføringer i Arena" })).toBeVisible();
  await expect(page.getByText("Må migreres", { exact: true })).toHaveCount(2);
  await expect(page.getByRole("button", { name: /^Migrer .* fra Arena$/ })).toHaveCount(2);
  expect([...new Set(oppslag)]).toEqual(["12345678901"]);

  await search.fill("opplæring");
  await expect(page.getByRole("button", { name: /^Migrer .* fra Arena$/ })).toHaveCount(0);
  await expect(page.getByText("Fant ingen tiltaksgjennomføringer", { exact: true })).toBeVisible();
  expect([...new Set(oppslag)]).toEqual(["12345678901"]);
});

test("identitetssøk tilbyr Arena-migrering ved siden av eksisterende enkeltplasser", async ({
  page,
}) => {
  const { oppslag } = await mockSok(page, { eksisterendeTreff: true });
  await page.getByRole("searchbox", { name: "Søk etter tiltaksgjennomføring" }).fill("12345678901");
  await expect(page.getByRole("button", { name: "Migrer 2026#42 fra Arena" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Eksisterende enkeltplass" })).toBeVisible();
  expect([...new Set(oppslag)]).toEqual(["12345678901"]);
});

test("Arena-oppslag uten kandidater viser ikke ekstra meldinger ved eksisterende treff", async ({
  page,
}) => {
  const { oppslag } = await mockSok(page, { eksisterendeTreff: true, kandidater: [] });
  await page.getByRole("searchbox", { name: "Søk etter tiltaksgjennomføring" }).fill("2026#42");
  await expect.poll(() => [...new Set(oppslag)]).toEqual(["2026#42"]);
  await expect(page.getByRole("link", { name: "Eksisterende enkeltplass" })).toBeVisible();
  await expect(page.getByRole("button", { name: /^Migrer .* fra Arena$/ })).toHaveCount(0);
  await expect(page.getByText("Fant ingen tiltaksgjennomføringer", { exact: true })).toHaveCount(0);
});

test("Arena-oppslaget beholder eksisterende treff uten å vise en lastemelding", async ({
  page,
}) => {
  let fullforOppslag!: () => void;
  const avventOppslag = new Promise<void>((resolve) => {
    fullforOppslag = resolve;
  });
  const { oppslag } = await mockSok(page, { eksisterendeTreff: true, avventOppslag });
  await page.getByRole("searchbox", { name: "Søk etter tiltaksgjennomføring" }).fill("12345678901");
  await expect.poll(() => oppslag).toContain("12345678901");
  await expect(page.getByText("Søker etter enkeltplasser i Arena", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("link", { name: "Eksisterende enkeltplass" })).toBeVisible();
  fullforOppslag();
  await expect(page.getByRole("button", { name: "Migrer 2026#42 fra Arena" })).toBeVisible();
});

test("feil i Arena-oppslaget skjuler ikke eksisterende enkeltplasser", async ({ page }) => {
  await mockSok(page, { eksisterendeTreff: true, oppslagsfeil: true });
  await page.getByRole("searchbox", { name: "Søk etter tiltaksgjennomføring" }).fill("12345678901");
  await expect(page.getByRole("alert")).toContainText(
    "Kunne ikke søke etter enkeltplasser i Arena. Prøv igjen.",
  );
  await expect(page.getByRole("link", { name: "Eksisterende enkeltplass" })).toBeVisible();
  await expect(page.getByRole("button", { name: /^Migrer .* fra Arena$/ })).toHaveCount(0);
});

test("gjør ikke Arena-oppslag uten handling fra backend selv med saksbehandlerrolle", async ({
  page,
}) => {
  const { oppslag } = await mockSok(page, { kanMigrere: false, harSaksbehandlerrolle: true });
  await page.getByRole("searchbox", { name: "Søk etter tiltaksgjennomføring" }).fill("12345678901");
  await expect(page.getByText("Fant ingen tiltaksgjennomføringer", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: /^Migrer .* fra Arena$/ })).toHaveCount(0);
  expect(oppslag).toEqual([]);
});

test("migreringshandling fra backend tilbyr migrering uten å sjekke roller i frontend", async ({
  page,
}) => {
  const { oppslag, migreringer } = await mockSok(page);
  await expect(page.getByRole("button", { name: /^Migrer .* fra Arena$/ })).toHaveCount(0);

  await page.getByRole("searchbox", { name: "Søk etter tiltaksgjennomføring" }).fill("2026/42");
  await page.getByRole("button", { name: "Migrer 2026#42 fra Arena" }).click();

  const modal = page.getByRole("dialog");
  await expect(modal.getByText("Opplæring fra Arena", { exact: true })).toBeVisible();
  await expect(modal.getByRole("textbox", { name: "Arena tiltaksnummer" })).toHaveCount(0);
  await modal.getByLabel("Prismodell").selectOption(PrismodellType.ANSKAFFET_ENKELTPLASS);
  await modal.getByLabel("Totalbeløp (NOK)").fill("4200");
  await modal.getByRole("button", { name: "Migrer enkeltplass" }).click();

  await expect
    .poll(() => migreringer)
    .toEqual([
      { type: "ANSKAFFET_ENKELTPLASS", totalbelop: 4200, prisbetingelser: null, tilskudd: {} },
    ]);
  expect([...new Set(oppslag)]).toEqual(["2026/42"]);
  await expect(page).toHaveURL(`/gjennomforinger/${kandidat.id}`);
});

test("migreringsskjemaet validerer påkrevde felt for valgt prismodell", async ({ page }) => {
  const { migreringer } = await mockSok(page);
  await page.getByRole("searchbox", { name: "Søk etter tiltaksgjennomføring" }).fill("2026/42");
  await page.getByRole("button", { name: "Migrer 2026#42 fra Arena" }).click();
  const modal = page.getByRole("dialog");
  const submit = modal.getByRole("button", { name: "Migrer enkeltplass" });

  await submit.click();
  await expect(modal.getByLabel("Prismodell")).toHaveAttribute("aria-invalid", "true");
  expect(migreringer).toEqual([]);

  await modal.getByLabel("Prismodell").selectOption(PrismodellType.ANSKAFFET_ENKELTPLASS);
  await submit.click();
  await expect(
    modal
      .getByRole("group", { name: "Velg prisinformasjon" })
      .getByText("Oppgi totalbeløp", { exact: true }),
  ).toBeVisible();
  expect(migreringer).toEqual([]);

  await modal.getByLabel("Prismodell").selectOption(PrismodellType.INGEN_KOSTNADER);
  await submit.click();
  await expect(
    modal
      .getByRole("group", { name: "Velg prisinformasjon" })
      .getByText("Oppgi begrunnelse", { exact: true }),
  ).toBeVisible();
  expect(migreringer).toEqual([]);

  await modal.getByLabel("Begrunnelse").fill("Plassen medfører ingen kostnader.");
  await submit.click();
  await expect
    .poll(() => migreringer)
    .toEqual([
      {
        type: "INGEN_KOSTNADER",
        totalbelop: null,
        prisbetingelser: "Plassen medfører ingen kostnader.",
        tilskudd: {},
      },
    ]);
});

test("valideringsfeil fra backend vises ved feltet og i skjemaets feiloppsummering", async ({
  page,
}) => {
  await mockSok(page, { migreringsfeil: true });
  await page.getByRole("searchbox", { name: "Søk etter tiltaksgjennomføring" }).fill("2026/42");
  await page.getByRole("button", { name: "Migrer 2026#42 fra Arena" }).click();
  const modal = page.getByRole("dialog");
  await modal.getByLabel("Prismodell").selectOption(PrismodellType.ANSKAFFET_ENKELTPLASS);
  await modal.getByLabel("Totalbeløp (NOK)").fill("4200");
  await modal.getByRole("button", { name: "Migrer enkeltplass" }).click();

  await expect(
    modal
      .getByRole("group", { name: "Velg prisinformasjon" })
      .getByText("Kontroller totalbeløpet", { exact: true }),
  ).toBeVisible();
  await expect(modal.getByLabel("Totalbeløp (NOK)")).toHaveAttribute("aria-invalid", "true");
  await modal.getByTitle("Se valideringsfeil").click();
  await expect(
    page.getByRole("heading", {
      name: "Du må rette følgende feil i skjemaet før du kan fortsette:",
    }),
  ).toBeVisible();
});

test("migrering sender estimerte beløp bare for valgte tilskuddstyper", async ({ page }) => {
  const { migreringer } = await mockSok(page);
  await page.getByRole("searchbox", { name: "Søk etter tiltaksgjennomføring" }).fill("2026/42");
  await page.getByRole("button", { name: "Migrer 2026#42 fra Arena" }).click();
  const modal = page.getByRole("dialog");

  await expect(modal.getByText("Arena tiltaksnummer: 2026#42", { exact: true })).toBeVisible();
  await expect(modal.getByRole("heading", { name: "Varighet" })).toBeVisible();
  await modal.getByLabel("Prismodell").selectOption(PrismodellType.TILSKUDD_TIL_OPPLAERING);
  await modal.getByRole("checkbox", { name: "Semesteravgift", exact: true }).check();
  await modal.getByRole("button", { name: "Migrer enkeltplass" }).click();
  await expect(
    modal
      .getByRole("group", {
        name: "Velg hvilke tilskudd som er aktuelle og anslå Navs utgifter",
      })
      .getByText("Oppgi estimert totalbeløp", { exact: true }),
  ).toBeVisible();
  expect(migreringer).toEqual([]);

  await modal.getByLabel("Estimert totalbeløp for semesteravgift (NOK)").fill("1500");
  await modal.getByRole("checkbox", { name: "Skolepenger", exact: true }).check();
  await modal.getByLabel("Estimert totalbeløp for skolepenger (NOK)").fill("3000");
  await modal.getByRole("checkbox", { name: "Eksamensgebyr", exact: true }).check();
  await modal.getByLabel("Estimert totalbeløp for eksamensgebyr (NOK)").fill("2000");
  await modal.getByRole("checkbox", { name: "Eksamensgebyr", exact: true }).uncheck();
  await expect(modal.getByText("Estimert totalsum").locator("..")).toContainText(/4\s*500/);
  await modal.getByRole("button", { name: "Migrer enkeltplass" }).click();

  await expect
    .poll(() => migreringer)
    .toEqual([
      {
        type: "TILSKUDD_TIL_OPPLAERING",
        totalbelop: null,
        prisbetingelser: null,
        tilskudd: { SEMESTERAVGIFT: 1500, SKOLEPENGER: 3000 },
      },
    ]);
});

test("tilskudd kan migreres uten estimerte beløp etter at alle typer fjernes", async ({ page }) => {
  const { migreringer } = await mockSok(page);
  await page.getByRole("searchbox", { name: "Søk etter tiltaksgjennomføring" }).fill("2026/42");
  await page.getByRole("button", { name: "Migrer 2026#42 fra Arena" }).click();
  const modal = page.getByRole("dialog");

  await modal.getByLabel("Prismodell").selectOption(PrismodellType.TILSKUDD_TIL_OPPLAERING);
  await modal.getByRole("checkbox", { name: "Semesteravgift", exact: true }).check();
  await modal.getByLabel("Estimert totalbeløp for semesteravgift (NOK)").fill("1500");
  await modal.getByRole("checkbox", { name: "Semesteravgift", exact: true }).uncheck();
  await modal.getByRole("button", { name: "Migrer enkeltplass" }).click();

  await expect
    .poll(() => migreringer)
    .toEqual([
      {
        type: "TILSKUDD_TIL_OPPLAERING",
        totalbelop: null,
        prisbetingelser: null,
        tilskudd: {},
      },
    ]);
});
