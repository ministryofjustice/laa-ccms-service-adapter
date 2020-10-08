function fetchXML(text) {
  return cy.request({
    url: 'http://localhost:8081/ccms/ws/opadrulebase',
    method: 'POST',
    body: text
  })
}

describe('The example means assessment requests for the Connector SOAP API', function () {
    it('returns the responses we expect', function () {
      cy.readFile(
          'cypress/integration/example-requests/billing-request.xml')
      .then(fetchXML)
      .then((response) => {
        expect(response.status).to.eq(200)
        const xml = Cypress.$.parseXML(response.body)
        expect(xml.getElementsByTagName('decision-report')).lengthOf(2)
        expect(xml.getElementsByTagName('entity')).lengthOf(56)
        expect(xml.getElementsByTagName('attribute-decision-node')).lengthOf(1194)
        expect(xml.getElementsByTagName('relationship')).lengthOf(68)
      })

    })
})
