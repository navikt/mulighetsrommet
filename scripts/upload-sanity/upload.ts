import {
  SanityKontaktperson,
  SanityArrangor,
  Block,
  SanityTiltaksgjennomforing,
  Tiltakstype,
  SanityTiltakstype,
  Lenke,
} from "./domain";
import { client } from "./sanityConfig";
const short = require("short-uuid");
var colors = require("colors");
import { faker } from "@faker-js/faker";

const fs = require("fs");
const { parse } = require("csv-parse");
const csvFil = "./tiltak.csv";
const skalLasteOpp = true;
const brukFakeData = true;

type Row = {
  [index: number]: string;
};

const kontaktpersoner: SanityKontaktperson[] = [];
const arrangorer: SanityArrangor[] = [];
const tiltaksgjennomforinger: Record<number, SanityTiltaksgjennomforing> = {};
const rows: Row[] = [];

//Om man trenger å slette noe fra sanity
async function deleteTyper(type: "tiltaksgjennomforing" | "tiltakstype") {
  await client.delete({
    query: `*[_type == "${type}"]`,
  });
  console.info("Slettet typer!");
  process.exit(1);
}

//deleteTyper("tiltakstype");

// Tiltakstypene er allerede definert i Sanity, så de kan vi hente før vi starter populering av csv-fil.
async function fetchTiltakstyperFraSanity(): Promise<SanityTiltakstype[]> {
  // TODO Fiks så vi kan hente tiltakstyper fra Sanity - Trenger bare hente én gang. Nå henter vi for hver rad i excel...
  console.log(colors.green("Henter tiltakstyper..."));
  const data = await client.fetch("*[_type == 'tiltakstype']");
  return Promise.resolve(data);
}

console.log(colors.green(`Leser ${csvFil} og laster rader opp til Sanity...`));

// Les tiltaksgjennomføringer fra csv-fil
fs.createReadStream(csvFil)
  .pipe(parse({ delimiter: ";", from_line: 2 }))
  .on("data", function (row: Row) {
    console.log(colors.green("✓ Leser rad fra csv-fil"));
    rows.push(row);
  })
  .on("end", async function () {
    console.log(colors.green(`Antall rader lest: ${rows.length}`));
    const tiltakstyper = await fetchTiltakstyperFraSanity();
    rows.forEach((row) => {
      opprettKontaktperson(row);
      opprettArrangor(row);
      opprettTiltaksgjennomforing(row, tiltakstyper);
    });

    console.log(
      colors.green(
        `Alle rader lest inn. ${
          skalLasteOpp
            ? "Laster opp til Sanity..."
            : "Opplasting til Sanity er ikke skrudd på!"
        } `
      )
    );

    const merged = mergeDokumenttyper(
      kontaktpersoner.map(fjernBrukerident),
      arrangorer,
      Object.values(tiltaksgjennomforinger)
    );

    if (skalLasteOpp) {
      lastOppDokumenter(merged);
      console.log(colors.green(`${merged.length} dokumenter lastet opp`));
    } else {
      console.log(
        colors.yellow(
          "Toggle for opplasting er ikke skrudd på. Ingen dokumenter ble lastet opp. Sett variabelen skalLasteOpp til true."
        )
      );
    }
  })
  .on("error", function (error: any) {
    console.log(colors.red(error.message));
  });

function opprettKontaktperson(row: Row): SanityKontaktperson {
  const navn = brukFakeData ? faker.name.findName() : row[17];
  const epost = brukFakeData ? faker.internet.email() : row[21];
  const ident = brukFakeData
    ? `${navn.substring(0, 2).toUpperCase()}${faker.random.numeric(6)}`
    : row[18];
  const telefonnummer = brukFakeData
    ? faker.phone.phoneNumber("### ## ###")
    : row[19];
  const enhet = brukFakeData ? faker.random.words(2) : row[21];

  const kontaktEksisterer = kontaktpersoner.find(
    (person) => person.ident === ident
  );
  if (kontaktEksisterer) return kontaktEksisterer;

  const person: SanityKontaktperson = {
    navn,
    ident,
    telefonnummer,
    enhet,
    epost,
    _id: ident,
    _type: "navKontaktperson",
  };

  kontaktpersoner.push(person);
  return person;
}

function opprettArrangor(row: Row): SanityArrangor {
  const navn = brukFakeData ? faker.company.companyName() : row[13];
  const telefon = brukFakeData
    ? faker.phone.phoneNumber("### ## ###")
    : row[14];
  const epost = brukFakeData ? faker.internet.email() : row[15];
  const postnr = brukFakeData ? faker.address.zipCode("####") : row[16];

  const arrangorEksisterer = arrangorer.find(
    (arrangor) => arrangor.epost === epost
  );
  if (arrangorEksisterer) return arrangorEksisterer;

  const arrangor: SanityArrangor = {
    _type: "arrangor",
    selskapsnavn: navn,
    telefonnummer: telefon,
    epost: epost,
    adresse: postnr,
    _id: epost.replace("@", "_"), // Sanity liker ikke @ i id'ene sine
  };
  arrangorer.push(arrangor);
  return arrangor;
}

function opprettTiltaksgjennomforing(
  row: Row,
  tiltakstyper: SanityTiltakstype[]
): SanityTiltaksgjennomforing {
  const tiltaksgjennomforingsnavn = brukFakeData
    ? faker.company.catchPhrase()
    : row[0];
  const beskrivelse = brukFakeData ? faker.lorem.paragraphs(2) : row[1];
  const tiltaksnummer = parseInt(row[2]);
  const tiltakstype = row[3] as Tiltakstype;
  const oppstart = row[4] !== "Løpende" ? "dato" : "lopende";
  const oppstartsdato =
    oppstart !== "lopende"
      ? new Date(row[4]).toISOString().substring(0, 10) // Hent ut dato på YYYY-MM-DD
      : undefined;
  const lokasjon = brukFakeData ? faker.address.street() : row[5];
  const forHvem = brukFakeData ? faker.lorem.paragraphs(2) : row[7];
  const detaljerOgInnhold = brukFakeData ? faker.lorem.paragraphs(2) : row[9];
  const pameldingOgVarighet = brukFakeData
    ? faker.lorem.paragraphs(2)
    : row[11];

  const arrangor = opprettArrangor(row);
  const kontaktinfoPerson = opprettKontaktperson(row);
  const tiltakstypeId = tiltakstyper.find(
    (type) => type.tiltakstypeNavn.toLowerCase() === tiltakstype.toLowerCase()
  )?._id;

  // Les ut lenker + lenkenavn, men bare ta med lenker som starter med https
  const lenker: Lenke[] = [
    opprettLenke(row[10], row[22]),
    opprettLenke(row[12], row[23]),
  ].filter((lenke) => lenke.lenke.startsWith("https"));

  if (!tiltakstypeId) {
    console.log(
      colors.red(`Fant ingen tiltakstypId for tiltakstype: ${tiltakstype}`)
    );
  }

  const gjennomforing: SanityTiltaksgjennomforing = {
    _id: tiltaksnummer.toString(),
    _type: "tiltaksgjennomforing",
    beskrivelse,
    tiltakstype: tiltakstypeId
      ? {
          _ref: tiltakstypeId,
          _type: "reference",
        }
      : null,
    tiltaksnummer: tiltaksnummer,
    oppstart: oppstart,
    oppstartsdato: oppstartsdato,
    tiltaksgjennomforingNavn: tiltaksgjennomforingsnavn,
    faneinnhold: {
      _type: "object",
      detaljerOgInnholdInfoboks: "",
      detaljerOgInnhold: addBlockContent(detaljerOgInnhold),
      forHvemInfoboks: "",
      forHvem: addBlockContent(forHvem),
      pameldingOgVarighetInfoboks: "",
      pameldingOgVarighet: addBlockContent(pameldingOgVarighet),
    },
    kontaktinfoTiltaksansvarlig: {
      _type: "reference",
      _ref: kontaktinfoPerson.ident,
    },
    kontaktinfoArrangor: {
      _ref: arrangor._id,
      _type: "reference",
    },
    lokasjon: lokasjon,
    lenker,
  };

  const tiltakstypeEksisterer = tiltaksgjennomforinger[tiltaksnummer];
  if (tiltakstypeEksisterer) return tiltakstypeEksisterer;

  tiltaksgjennomforinger[tiltaksnummer] = gjennomforing;

  return gjennomforing;
}

function addBlockContent(tekst: string): Block[] {
  return [
    {
      style: "normal",
      _type: "block",
      _key: short.generate(),
      markDefs: [],
      children: [
        { _type: "span", _key: short.generate(), marks: [], text: tekst },
      ],
    },
  ];
}

function mergeDokumenttyper(
  kontaktpersoner: SanityKontaktperson[],
  arrangorer: SanityArrangor[],
  tiltaksgjennomforinger: SanityTiltaksgjennomforing[]
): any[] {
  return [...kontaktpersoner, ...arrangorer, ...tiltaksgjennomforinger];
}

function lastOppDokumenter(dokumenter: any[]) {
  const transaction = client.transaction();
  dokumenter.forEach((person) => transaction.createOrReplace(person));
  transaction.commit();
}

function fjernBrukerident(person: SanityKontaktperson): SanityKontaktperson {
  delete person.ident;
  return person;
}

function beholdLenkeHvisStarterMedHttps(
  data: string,
  fallback: string
): string {
  return data.startsWith("https") ? data : fallback;
}

function opprettLenke(url: string, lenkenavn: string) {
  return {
    _key: short.generate(),
    lenke: brukFakeData
      ? faker.internet.url()
      : beholdLenkeHvisStarterMedHttps(url, ""),
    lenkenavn: brukFakeData
      ? faker.company.catchPhrase()
      : lenkenavn?.trim().length > 0
      ? lenkenavn
      : beholdLenkeHvisStarterMedHttps(url, ""),
  };
}
