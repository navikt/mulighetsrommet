import {
  SanityKontaktperson,
  SanityArrangor,
  Block,
  SanityTiltaksgjennomforing,
} from "./domain";
import { client } from "./sanityConfig";
const short = require("short-uuid");
var colors = require("colors");

const fs = require("fs");
const { parse } = require("csv-parse");
const csvFil = "./tiltak.csv";
const skalLasteOpp = true;

// client.delete({
//   query: `*[_type == "tiltaksgjennomforing"]`,
// });

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
    opprettKontaktperson(row);
    opprettArrangor(row);
    // TODO Hardkode tiltakstyper og mappe de til korrekte string-verdier for
    // automatisk kobling mellom tiltaksgjennomføringer og tiltakstyper
    opprettTiltaksgjennomforing(row);
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
      console.log(`Ville ha lastet opp:\n ${JSON.stringify(merged, null, 2)}`);
    }
  })
  .on("error", function (error: any) {
    console.log(colors.red(error.message));
  });

function opprettKontaktperson(row: Row): SanityKontaktperson {
  const navn = row[17];
  const ident = row[18];
  const telefonnummer = row[19];
  const epost = row[20];
  const enhet = row[21];

  const kontaktEksisterer = kontaktpersoner.find(
    (person) => person.ident === ident
  );
  if (kontaktEksisterer) return kontaktEksisterer;

  const person: SanityKontaktperson = {
    navn,
    ident,
    telefonnummer,
    epost,
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

function opprettTiltaksgjennomforing(row: Row): SanityTiltaksgjennomforing {
  const tiltaksgjennomforingsnavn = row[0];
  const tiltaksnummer = parseInt(row[2]);
  const oppstartsdato = new Date(row[4]).toISOString().substring(0, 10); // Hent ut dato på YYYY-MM-DD
  const lokasjon = row[5];
  const forHvem = row[7];
  const detaljerOgInnhold = row[9];
  const pameldingOgVarighet = row[11];

  const arrangor = opprettArrangor(row);
  const kontaktinfoPerson = opprettKontaktperson(row);

  const gjennomforing: SanityTiltaksgjennomforing = {
    _id: tiltaksnummer.toString(),
    _type: "tiltaksgjennomforing",
    tiltaksnummer: tiltaksnummer,
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
