import {
  SanityKontaktperson,
  SanityArrangor,
  Block,
  SanityTiltaksgjennomforing,
  Tiltakstype,
  SanityTiltakstype,
} from "./domain";
import { client } from "./sanityConfig";
const short = require("short-uuid");
var colors = require("colors");

const fs = require("fs");
const { parse } = require("csv-parse");
const csvFil = "./tiltak.csv";
const skalLasteOpp = false;

// Om man trenger å slette noe fra sanity
// client.delete({
//   query: `*[_type == "tiltaksgjennomforing"]`,
// });

// Tiltakstypene er allerede definert i Sanity, så de kan vi hente før vi starter populering av csv-fil.
function fetchTiltakstyperFraSanity(): Promise<SanityTiltakstype[]> {
  console.log(colors.green("Henter tiltakstyper..."));
  return client.fetch("*[_type == 'tiltakstype']");
}

type Row = {
  [index: number]: string;
};

const kontaktpersoner: SanityKontaktperson[] = [];
const arrangorer: SanityArrangor[] = [];
const tiltaksgjennomforinger: Record<number, SanityTiltaksgjennomforing> = {};

console.log(colors.green(`Leser ${csvFil} og laster rader opp til Sanity...`));

// Les tiltaksgjennomføringer fra csv-fil
fs.createReadStream(csvFil)
  .pipe(parse({ delimiter: ";", from_line: 2 }))
  .on("data", function (row: Row) {
    console.log(colors.green("✓ Leser rad fra csv-fil"));
    fetchTiltakstyperFraSanity().then((tiltakstyper: SanityTiltakstype[]) => {
      opprettKontaktperson(row);
      opprettArrangor(row);
      opprettTiltaksgjennomforing(row, tiltakstyper);
    });
  })
  .on("end", function () {
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
      // console.log(`Ville ha lastet opp:\n ${JSON.stringify(merged, null, 2)}`);
    }
  })
  .on("error", function (error: any) {
    console.log(colors.red(error.message));
  });

function opprettKontaktperson(row: Row): SanityKontaktperson {
  const navn = row[17];
  const ident = row[18];
  const telefonnummer = row[19];
  const enhet = row[21];

  const kontaktEksisterer = kontaktpersoner.find(
    (person) => person.ident === ident
  );
  if (kontaktEksisterer) return kontaktEksisterer;

  const person: SanityKontaktperson = {
    navn,
    ident,
    telefonnummer,
    enhet,
    _id: ident,
    _type: "navKontaktperson",
  };

  kontaktpersoner.push(person);
  return person;
}

function opprettArrangor(row: Row): SanityArrangor {
  const navn = row[13];
  const telefon = row[14];
  const epost = row[15];
  const postnr = row[16];

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
  const tiltaksgjennomforingsnavn = row[0];
  const tiltaksnummer = parseInt(row[2]);
  const tiltakstype = row[3] as Tiltakstype;
  const oppstart = row[4];
  console.log({ oppstart });
  const oppstartsdato =
    oppstart !== "Løpende"
      ? new Date(row[4]).toISOString().substring(0, 10)
      : null; // Hent ut dato på YYYY-MM-DD
  const lokasjon = row[5];
  const forHvem = row[7];
  const detaljerOgInnhold = row[9];
  const pameldingOgVarighet = row[11];

  const arrangor = opprettArrangor(row);
  const kontaktinfoPerson = opprettKontaktperson(row);
  const tiltakstypeId = tiltakstyper.find(
    (type) => type.tiltakstypeNavn.toLowerCase() === tiltakstype.toLowerCase()
  )?._id;

  if (!tiltakstypeId) {
    console.log(
      colors.red(`Fant ingen tiltakstypId for tiltakstype: ${tiltakstype}`)
    );
  }

  const gjennomforing: SanityTiltaksgjennomforing = {
    _id: tiltaksnummer.toString(),
    _type: "tiltaksgjennomforing",
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
      _type: "document",
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
    beskrivelse: "",
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
