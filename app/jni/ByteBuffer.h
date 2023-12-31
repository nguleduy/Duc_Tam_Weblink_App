//////////////////////////////////////////////////////////////////////////////////////////////
///
/// @file ByteBuffer.h
/// @brief
///
/// Contains declaration of the CByteBuffer class.
///
/// @author Abalta Technologies, Inc.
/// @date 8/2011
///
/// @cond Copyright
///
/// COPYRIGHT 2011 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
/// This program may not be reproduced, in whole or in part in any form or any means whatsoever
/// without the written permission of ABALTA TECHNOLOGIES.
///
/// @endcond
//////////////////////////////////////////////////////////////////////////////////////////////

#ifndef __ByteBuffer_H__
#define __ByteBuffer_H__

//--------------------------------------------------------------------------------------------
//  Includes
//--------------------------------------------------------------------------------------------
#include <stdlib.h>
#include <assert.h>
#include <string.h>

//--------------------------------------------------------------------------------------------
//  Type definitions
//--------------------------------------------------------------------------------------------


//////////////////////////////////////////////////////////////////////////////////////////////
/// \class CByteBuffer
/// \brief Wrapper for char buffer. Contains char pointer, buffer size and current position in the
/// buffer.
//////////////////////////////////////////////////////////////////////////////////////////////
class CByteBuffer
{
public:
  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Default constructor
  //////////////////////////////////////////////////////////////////////////////////////////////
  CByteBuffer();

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Copy constructor
  ///
  /// @param[in] buffer - the source buffer
  //////////////////////////////////////////////////////////////////////////////////////////////
  CByteBuffer(const CByteBuffer& buffer);

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Construct a buffer with existing data
  ///
  /// @param[in] buf         - the source data buffer
  /// @param[in] size        - the buffer size
  /// @param[in] shallowCopy - if true then reference the passed buffer, otherwise makea copy
  //////////////////////////////////////////////////////////////////////////////////////////////
  CByteBuffer(const char* buf, unsigned size, bool shallowCopy = false);

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Construct a buffer with specified size
  ///
  /// @param[in] size        - the buffer size
  //////////////////////////////////////////////////////////////////////////////////////////////
  CByteBuffer(unsigned size);

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Destructor
  //////////////////////////////////////////////////////////////////////////////////////////////
  virtual ~CByteBuffer();

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Sets the buffer
  ///
  /// @param[in] buf         - the source data buffer
  /// @param[in] size        - the buffer size
  /// @param[in] shallowCopy - if true then reference the passed buffer, otherwise makea copy
  //////////////////////////////////////////////////////////////////////////////////////////////
  bool        SetBuffer(const char* buf, unsigned size, bool shallowCopy = false);

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Takes ownership if the buffer is a shallow copy
  //////////////////////////////////////////////////////////////////////////////////////////////
  void        TakeOwnership();

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Returns the buffer data
  ///
  /// @retval const char* - the buffer data
  //////////////////////////////////////////////////////////////////////////////////////////////
  const char* GetData() const;

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Returns the buffer data
  ///
  /// @retval char* - the buffer data
  //////////////////////////////////////////////////////////////////////////////////////////////
  char*       GetData();

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Removes the specified number of bytes from the buffer start
  ///
  /// @param[in] bytesToDiscard - bytes to be removed
  /// @retval unsigned - the number of bytes actually discarded
  //////////////////////////////////////////////////////////////////////////////////////////////
  unsigned    DiscardBytesFromStart(unsigned bytesToDiscard);

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Copies the specified number of bytes to the output and removes them from the buffer
  ///
  /// @param[out] buffer  - the output buffer
  /// @param[in]  bufSize - number of bytes to read
  /// @retval unsigned - the number of bytes actually read
  //////////////////////////////////////////////////////////////////////////////////////////////
  unsigned    GetBytes(char buffer[], unsigned bufSize);

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Copies the specified number of bytes to the output
  ///
  /// @param[in]  offset  - the the starting offset
  /// @param[out] buffer  - the output buffer
  /// @param[in]  bufSize - number of bytes to read
  /// @retval unsigned - the number of bytes actually read
  //////////////////////////////////////////////////////////////////////////////////////////////
  unsigned    PeekBytes(unsigned offset, char buffer[], unsigned bufSize) const;

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Adds bytes to the buffer
  ///
  /// @param[in] buffer  - the input data
  /// @param[in] bufSize - number of bytes to to add
  /// @retval bool - true on success, false otherwise
  //////////////////////////////////////////////////////////////////////////////////////////////
  bool        AddBytes(const char buffer[], unsigned bufSize);

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Resizes the buffer
  ///
  /// @param[in] size - the new buffer size
  /// @retval bool - true on success, false otherwise
  //////////////////////////////////////////////////////////////////////////////////////////////
  bool        Resize(unsigned size);

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Returns the buffer size
  /// @retval unsigned - the buffer size
  //////////////////////////////////////////////////////////////////////////////////////////////
  unsigned    GetSize() const;

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Check if buffer is empty
  /// @retval bool - true if buffer is empty, false otherwise
  //////////////////////////////////////////////////////////////////////////////////////////////
  bool        IsEmpty() const;

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Returns the buffer capacity
  /// @retval unsigned - the buffer size
  //////////////////////////////////////////////////////////////////////////////////////////////
  unsigned    GetCapacity() const;

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Sets the buffer capacity
  ///
  /// @param[in] capacity - the new capacity
  /// @retval bool - true on success, false otherwise
  //////////////////////////////////////////////////////////////////////////////////////////////
  bool        SetCapacity(unsigned capacity);

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Resets the buffer.
  //////////////////////////////////////////////////////////////////////////////////////////////
  void        Reset();

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// Normalizes the buffer. When data is removed from the start, just the offset is increased.
  /// Normalization means the the offset is set to 0 and contained data is moved to the buffer start
  ///
  /// @retval bool - true on success, false otherwise
  //////////////////////////////////////////////////////////////////////////////////////////////
  bool        Normalize();

protected:
  bool CopyOnWrite();

  // protected properties:
protected:
  char*    m_buffer;
  unsigned m_size;
  unsigned m_pos;
  unsigned m_capacity;
  bool     m_hasOwnership;
};

inline void CByteBuffer::Reset()
{
  m_size = 0;
  m_pos  = 0;
  if (!m_hasOwnership)
  {
    m_hasOwnership = true;
    m_buffer   = NULL;
    m_capacity = 0;
  }
}

inline char* CByteBuffer::GetData()
{
  return m_buffer + m_pos;
}

inline const char* CByteBuffer::GetData() const
{
  return m_buffer + m_pos;
}

// discard bytes from the buffer start
inline unsigned CByteBuffer::DiscardBytesFromStart(unsigned bytesToDiscard)
{
  unsigned result = GetSize();
  if (bytesToDiscard > result)
  {
    bytesToDiscard = result;
  }
  m_pos += bytesToDiscard;
  return bytesToDiscard;
}

inline unsigned CByteBuffer::GetBytes(char buffer[], unsigned bufSize)
{
  unsigned result = PeekBytes(0, buffer, bufSize);
  if (result > 0)
  {
    m_pos += result;
    if (m_pos == m_size)
    {
      m_pos = m_size = 0;
    }
  }
  return result;
}

inline unsigned CByteBuffer::PeekBytes(unsigned offset, char buffer[], unsigned bufSize) const
{
  int length = m_size - m_pos - offset;

  if ((int)bufSize < length)
  {
    length = bufSize;
  }
  if (length > 0)
  {
    assert(buffer);
    memcpy(buffer + offset, m_buffer + m_pos, length);
  }
  return length;
}

inline bool CByteBuffer::IsEmpty() const
{
  return !m_buffer || m_pos >= m_size;
}

inline unsigned CByteBuffer::GetSize() const
{
  return m_size - m_pos;
}

inline unsigned CByteBuffer::GetCapacity() const
{
  return m_capacity;
}

#endif //__ByteBuffer_H__
