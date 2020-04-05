package jp.co.layerx.cordage.crosschainatomicswap.flow

import jp.co.layerx.cordage.crosschainatomicswap.contract.SecurityContract
import jp.co.layerx.cordage.crosschainatomicswap.state.SecurityState
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test

class SecurityIssueFlowTest {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setUp() {
        network = MockNetwork(
            listOf("jp.co.layerx.cordage.crosschainatomicswap"),
            notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB")))
        )
        a = network.createNode(MockNodeParameters(legalName = CordaX500Name("Alice", "London", "GB")))
        b = network.createNode(MockNodeParameters(legalName = CordaX500Name("Bob", "London", "GB")))
        c = network.createNode(MockNodeParameters(legalName = CordaX500Name("Charlie", "London", "GB")))
        val startedNodes = arrayListOf(a, b, c)
        startedNodes.forEach { it.registerInitiatedFlow(SecurityIssueFlowResponder::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `normal scenario`() {
        val amount = 100
        val owner = a.info.legalIdentities.single()
        val issuer = c.info.legalIdentities.single()
        val securityName = "R3"
        val flow = SecurityIssueFlow(amount, owner,securityName)
        val future = c.startFlow(flow)
        network.runNetwork()
        val response = future.getOrThrow()

        val actualSignedTx = response
        val expected = SecurityState(amount, owner, issuer, securityName)
        Assertions.assertThat(actualSignedTx.inputs.isEmpty())
        Assertions.assertThat(actualSignedTx.tx.outputStates.single() == expected)

        val command = actualSignedTx.tx.commands.single()
        Assertions.assertThat(command.value is SecurityContract.SecurityCommands.Issue)
        Assertions.assertThat(command.signers.toSet() == expected.participants.map { it.owningKey }.toSet())
        actualSignedTx.verifyRequiredSignatures()
    }
}
