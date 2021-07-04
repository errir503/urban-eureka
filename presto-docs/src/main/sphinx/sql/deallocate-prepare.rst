==================
DEALLOCATE PREPARE
==================

Synopsis
--------

.. code-block:: none

    DEALLOCATE PREPARE statement_name

Description
-----------

Removes a statement with the name ``statement_name`` from the list of prepared
statements in a session.

Examples
--------

Deallocate a statement with the name ``my_query``::

    DEALLOCATE PREPARE my_query;

See Also
--------
:doc:`prepare`
