export function utledDelMedBrukerTekst(
  originaldeletekstFraTiltakstypen: string,
  tiltaksgjennomforingsnavn: string,
  brukernavn?: string,
  veiledernavn?: string,
) {
  const intro = `Hei ${brukernavn}`;

  const tiltak = originaldeletekstFraTiltakstypen.replaceAll(
    "<tiltaksnavn>",
    tiltaksgjennomforingsnavn,
  );

  const hilsen = hilsenTekst(veiledernavn);
  return `${intro}\n\n${tiltak}\n\n${hilsen}`;
}

function hilsenTekst(veiledernavn?: string) {
  const interessant = "Er dette aktuelt for deg? Gi meg tilbakemelding her i dialogen.";
  return veiledernavn
    ? `${interessant}\n\nVi holder kontakten!\nHilsen ${veiledernavn}`
    : `${interessant}\n\nVi holder kontakten!`;
}
