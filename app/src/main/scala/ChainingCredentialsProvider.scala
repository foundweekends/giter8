package giter8

import org.eclipse.jgit.errors.UnsupportedCredentialItem
import org.eclipse.jgit.transport.{CredentialItem, CredentialsProvider, URIish}

/**
 * A credentials provider chaining multiple credentials providers.
 *
 * This is a Scala translation of the JGit ChainingCredentialsProvider (since JGit 3.5)
 * with bug fixes to make the chaining really work. Please remove this file and import the JGit
 * ChainingCredentialsProvider into JGit.scala once the bug in JGit is fixed.
 *
 * Bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=469554
 */
class ChainingCredentialsProvider(val providers: CredentialsProvider*) extends CredentialsProvider {

  /**
   * @return { @code true} if any of the credential providers in the list is
   *                 interactive, otherwise { @code false}
   * @see org.eclipse.jgit.transport.CredentialsProvider#isInteractive()
   */
  def isInteractive: Boolean = providers exists (_.isInteractive)

  /**
   * @return { @code true} if any of the credential providers in the list
   *                 supports the requested items, otherwise { @code false}
   * @see org.eclipse.jgit.transport.CredentialsProvider#supports(org.eclipse.jgit.transport.CredentialItem[])
   */
  def supports(items: CredentialItem*): Boolean = providers exists (_.supports(items: _*))

  /**
   * Populates the credential items with the credentials provided by the first
   * credential provider in the list which populates them with non-null values
   *
   * @return { @code true} if any of the credential providers in the list
   *                 supports the requested items, otherwise { @code false}
   * @see org.eclipse.jgit.transport.CredentialsProvider#supports(org.eclipse.jgit.transport.CredentialItem[])
   */
  @throws(classOf[UnsupportedCredentialItem])
  def get(uri: URIish, items: CredentialItem*): Boolean = providers exists { provider =>
    if (provider.supports(items: _*)) {
      provider.get(uri, items: _*)
      !isAnyNull(items: _*)
    } else false
  }

  private def isAnyNull(items: CredentialItem*): Boolean = {
    items exists {
      case null => true
      case item: CredentialItem.StringType    if item.getValue == null => true
      case item: CredentialItem.CharArrayType if item.getValue == null => true
      case _ => false
    }
  }
}