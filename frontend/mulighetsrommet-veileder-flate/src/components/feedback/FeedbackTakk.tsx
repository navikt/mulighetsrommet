import * as React from 'react';
import { BodyShort } from '@navikt/ds-react';

function TilbakemeldingTakkModal() {
  return (
    <div className="tilbakemelding-modal__takk-melding">
      <img
        alt="Takk for din tilbakemelding"
        className="tilbakemelding-modal__takk-ikon"
        src="data:img/png;base64,iVBORw0KGgoAAAANSUhEUgAAADQAAAA0CAMAAADypuvZAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAABQVBMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABBQMAAAAAAAAAAAADCgYAAAAAAAACBgMAAAAAAAAAAQEAAAAAAAAFEQoAAAAAAAAAAAARMR0fWjYoc0UwilMzlVk2nV4VPCQmbkI1ml04oWEbTi81mFw0mFsWQCYykVcsgE0MIhQUOiI2m10xjlUIGA4JHBEIGQ8TOCIbTy8GEQoQMB0BBQMSNR8JGxAcUjEiYzwEDAcyklguhVA0lloXQigvh1EviFIXQygKHhINJhc3n18PLRsKHREqekkAAgEYRiogXDcnckQgXjgeWDUqeUkBBAIYRyoUOyQ0lVojZT0NJxcgXjkDCgYtg08ncEMwjFQBAwEDCQUMIxUHFAwlakAjZj4ZRysSMx8QLhwTOSIbTS4re0o3n2CSKFGVAAAAHXRSTlMAJWaVvN7s+S+M4jbA/r8kqv4E+v4Tvv4r3f5F8rmg0BYAAAABYktHRACIBR1IAAAACXBIWXMAAAsSAAALEgHS3X78AAAAB3RJTUUH4wIEDg4gBtUn9wAAAjlJREFUSMedVmdj2jAUFBuTQtgEwrFBEMxqQhLIJB1p0126917//wfUCEolGbDRfTvxDktvE2KGw+lye7w+n9fjdjkdxAb8WgACAprfQrJxzTAL5vKFYqlcLhUL+VzQOAhtrJBshoFIpVqjHGrVSgQIby6RRLUY4vUGNWGnHkdMiy7SJJJINXW6EHozhWTCrNlKI9OiS9HKIL0la7azaOt0BfQ2stvSd7LodOlKdDvICt9KpNGhluggzb0rmkS7ay3qtpH870MNGd1aY7wrA20e01iqZUdj+DAV+xflMJr2NJQ2EZ7lG+K2LscuGMc0D0Oo29VQWkeI1QIifL71ru/2eKve7h7PdyLwM9dVeKM+sM/zfaDP8wpzYABV/vAAOOT5IXDA8yoCRm0jKNTPABjyfAgMeF4LwkGcyAkvPQKOeX4MHAkGOTiJC3nh7ASnZzw/O8WJYJCHi7hREJ16PhL56FzkBbiJB0WbIZqhCA/xorSeqAQv8aG8nqgMn5pI6XpKjjC53AoTl8vBtcQkuHIaWWKSRnLCXtyQrW7e4hlLWLk0bl/ekUR3r3jGSkMuwnv3H4iah3jE02kRSuVOH+MJT5+OhT9pTMvd1Fie4fmLOXk5fiVkzKyxmFvY6/Gb+lv26HeXGAr9et7CzM3yff8DPl592hvg8xfxl3mzXNSWR1+/ff/x89fvP+Ix15aVBoDaqFEaamrjU21Qq60EasvHijWnsXzNIUoLFcvD9Vc3NhjXXxIZLNfRv/8qPkEO8w3mAAAAAElFTkSuQmCC"
      />
      <BodyShort size="small">
        Takk for at du tok deg tid til å gi tilbakemelding. Vi bruker innspillene til å forbedre løsningen.
      </BodyShort>
    </div>
  );
}

export default TilbakemeldingTakkModal;
